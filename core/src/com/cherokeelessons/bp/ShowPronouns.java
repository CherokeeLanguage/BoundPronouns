package com.cherokeelessons.bp;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.cherokeelessons.bp.BoundPronouns.Font;
import com.cherokeelessons.bp.build.CherokeeUtils;
import com.cherokeelessons.bp.build.DataSet;

public class ShowPronouns extends ChildScreen {

	private static class DisplayRecord implements Comparable<DisplayRecord> {
		public enum SortBy {
			Syllabary, Latin, Definition;
		}

		public enum SortOrder {
			Ascending, Descending, SplitAscending, SplitDescending;
		}

		private static SortBy by = SortBy.Syllabary;
		private static SortOrder order = SortOrder.Ascending;

		public static void setSortBy(final SortBy by) {
			if (by == null) {
				return;
			}
			if (DisplayRecord.by.equals(by)) {
				int nextOrdinal = DisplayRecord.order.ordinal() + 1;
				if (!by.equals(DisplayRecord.SortBy.Definition)) {
					nextOrdinal %= 2;
				} else {
					nextOrdinal %= DisplayRecord.SortOrder.values().length;
				}
				DisplayRecord.order = DisplayRecord.SortOrder.values()[nextOrdinal];
			} else {
				DisplayRecord.by = by;
				DisplayRecord.order = DisplayRecord.SortOrder.values()[0];
			}
		}

		public static void setSortSubOrder(final SortOrder order) {
			if (order == null) {
				return;
			}
			DisplayRecord.order = order;
		}

		public Label syllabary;
		public Label latin;
		public Label definition;

		private String cleanup(String string) {
			string = string.replace(BoundPronouns.UNDERDOT, "");
			string = string.replace(BoundPronouns.UNDERX, "");
			string = StringUtils.replaceChars(string, "ạẹịọụṿẠẸỊỌỤṾ", "aeiouvAEIOUV");
			string = string.replaceAll("[¹²³⁴]", "");
			string = StringUtils.normalizeSpace(string);
			string = StringUtils.strip(string);
			return string;
		}

		@Override
		public int compareTo(final DisplayRecord o) {
			switch (DisplayRecord.order) {
			case Ascending:
				if (o == null) {
					return 1;
				}
				return sortKey().compareTo(o.sortKey());
			case Descending:
				if (o == null) {
					return -1;
				}
				return -sortKey().compareTo(o.sortKey());
			case SplitAscending:
				if (o == null) {
					return 1;
				}
				return splitCompare(o);
			case SplitDescending:
				if (o == null) {
					return -1;
				}
				return -splitCompare(o);
			default:
				return sortKey().compareTo(o.sortKey());
			}
		}

		private String sortKey() {
			switch (by) {
			case Definition:
				String string = definition.getText().toString() + "|" + syllabary.getText().toString() + "|"
						+ CherokeeUtils.ced2mco_nfc(latin.getText().toString());
				string = cleanup(string);
				return string.toLowerCase();
			case Latin:
				String string2 = CherokeeUtils.ced2mco_nfc(latin.getText().toString()) + "|" + definition.getText().toString() + "|"
						+ syllabary.getText().toString();
				string2 = cleanup(string2);
				return string2.toLowerCase();
			case Syllabary:
				String string3 = CherokeeUtils.ced2mco_nfc(syllabary.getText().toString()) + "|" + latin.getText().toString() + "|"
						+ definition.getText().toString();
				string3 = cleanup(string3);
				return string3.toLowerCase();
			default:
				return "";
			}
		}

		public int splitCompare(final DisplayRecord o) {
			final String[] x1 = sortKey().split("\\|");
			final String[] x2 = o.sortKey().split("\\|");
			final StringBuilder sb1 = new StringBuilder();
			final StringBuilder sb2 = new StringBuilder();
			for (final String x : x1) {
				sb1.append(StringUtils.substringAfter(x, "+"));
				sb1.append("+");
				sb1.append(StringUtils.substringBefore(x, "+"));
				sb1.append("|");
			}
			for (final String x : x2) {
				sb2.append(StringUtils.substringAfter(x, "+"));
				sb2.append("+");
				sb2.append(StringUtils.substringBefore(x, "+"));
				sb2.append("|");
			}
			return sb1.toString().compareTo(sb2.toString());
		}
	}

	private static final String SORT_BY_ENGLISH = "Sort by English";
	private static final String SORT_BY_LATIN = "Sort by Latin";
	private static final String SORT_BY_SYLLABARY = "Sort by Syllabary";

	private final Array<DisplayRecord> drecs = new Array<>();

	private final Table table;
	private final ClickListener list_sortByD = new ClickListener() {
		@Override
		public boolean touchDown(final InputEvent event, final float x, final float y, final int pointer,
				final int button) {
			DisplayRecord.setSortBy(DisplayRecord.SortBy.Definition);
			drecs.sort();
			populateList();
			return true;
		}
	};
	private final ClickListener list_sortByL = new ClickListener() {
		@Override
		public boolean touchDown(final InputEvent event, final float x, final float y, final int pointer,
				final int button) {
			DisplayRecord.setSortBy(DisplayRecord.SortBy.Latin);
			drecs.sort();
			populateList();
			return true;
		}
	};
	private final ClickListener list_sortByS = new ClickListener() {
		@Override
		public boolean touchDown(final InputEvent event, final float x, final float y, final int pointer,
				final int button) {
			DisplayRecord.setSortBy(DisplayRecord.SortBy.Syllabary);
			drecs.sort();
			populateList();
			return true;
		}
	};
	private TextButton sortByS;
	private TextButton sortByL;
	private TextButton sortByD;
	private ScrollPane scroll;
	private final Skin skin;
	private final Table container;

	public ShowPronouns(final BoundPronouns game, final Screen callingScreen) {
		super(game, callingScreen);

		skin = game.manager.get(BoundPronouns.SKIN, Skin.class);
		table = new Table();
		container = new Table(skin);
		stage.addActor(container);
		final Texture texture = game.manager.get(BoundPronouns.IMG_MAYAN, Texture.class);
		final TiledDrawable d = new TiledDrawable(new TextureRegion(texture));
		container.setBackground(d);
		container.setFillParent(true);
		Gdx.app.postRunnable(new Runnable() {
			@Override
			public void run() {
				initialPopulate(game);
			}
		});

	}

	@Override
	public void dispose() {
		drecs.clear();
		super.dispose();
	}

	private String getIndicator(final com.cherokeelessons.bp.ShowPronouns.DisplayRecord.SortBy by) {
		if (!DisplayRecord.by.equals(by)) {
			return "   ";
		}
		switch (DisplayRecord.order) {
		case Ascending:
			return " " + BoundPronouns.TRIANGLE_ASC + " ";
		case Descending:
			return " " + BoundPronouns.TRIANGLE_DESC + " ";
		case SplitAscending:
			return " " + BoundPronouns.TRIANGLE_ASC + BoundPronouns.DIAMOND;
		case SplitDescending:
			return " " + BoundPronouns.TRIANGLE_DESC + BoundPronouns.DIAMOND;
		default:
			return "   ";
		}
	}

	public void initialPopulate(@SuppressWarnings("hiding") final BoundPronouns game) {
		final Texture texture = game.manager.get(BoundPronouns.IMG_MAYAN, Texture.class);
		final TiledDrawable d = new TiledDrawable(new TextureRegion(texture));
		final BitmapFont font_base = game.getFont(Font.SerifXSmall);

		final TextButtonStyle bstyle = new TextButtonStyle(skin.get("default", TextButtonStyle.class));
		bstyle.font = font_base;
		container.row();
		final TextButtonStyle bls = new TextButtonStyle(bstyle);
		bls.fontColor = Color.BLUE;
		final TextButton back = new TextButton(BoundPronouns.BACK_ARROW, bls);
		container.add(back).center().width(BoundPronouns.BACK_WIDTH);
		back.addListener(exit);

		sortByS = new TextButton(SORT_BY_SYLLABARY, bstyle);
		sortByS.addListener(list_sortByS);
		container.add(sortByS).center().fill().expand();

		sortByL = new TextButton(SORT_BY_LATIN, bstyle);
		sortByL.addListener(list_sortByL);
		container.add(sortByL).center().fill().expand();

		sortByD = new TextButton(SORT_BY_ENGLISH, bstyle);
		sortByD.addListener(list_sortByD);

		final int c = container.add(sortByD).center().fill().expand().getColumn();

		table.setBackground(d);

		scroll = new ScrollPane(table, skin);
		scroll.setColor(Color.DARK_GRAY);
		scroll.setFadeScrollBars(false);
		scroll.setSmoothScrolling(true);
		container.row();
		container.add(scroll).expand().fill().colspan(c + 1);

		final LabelStyle ls = new LabelStyle(skin.get("default", LabelStyle.class));
		ls.font = font_base;
		ls.background = null;

		final List<DataSet> list = BoundPronouns.loadPronounRecords();

		for (final DataSet data : list) {
			final DisplayRecord dr = new DisplayRecord();

			Label actor;

			actor = new Label(data.chr, ls);
			dr.syllabary = actor;

			actor = new Label(CherokeeUtils.ced2mco_nfc(data.latin), ls);
			dr.latin = actor;

			actor = new Label(data.def, ls);
			dr.definition = actor;

			drecs.add(dr);
		}

		DisplayRecord.setSortBy(DisplayRecord.SortBy.Syllabary);
		DisplayRecord.setSortSubOrder(DisplayRecord.SortOrder.Ascending);
		drecs.sort();
		populateList();
	}

	private void populateList() {
		int ix = 0;
		table.clear();
		for (final DisplayRecord rec : drecs) {
			ix++;
			final boolean greenbar = ix % 3 == 0;
			table.row();
			final Cell<Label> cell_syll = table.add(rec.syllabary);
			cell_syll.align(Align.left).padLeft(12).padRight(6).padBottom(5).expandX();
			final Cell<Label> cell_latin = table.add(rec.latin);
			cell_latin.align(Align.left).padRight(6).padBottom(5).expandX();
			final Cell<Label> cell_def = table.add(rec.definition);
			cell_def.align(Align.left).padBottom(5).expandX();
			if (greenbar) {
				cell_syll.padBottom(40);
				cell_latin.padBottom(40);
				cell_def.padBottom(40);
			}
		}
		updateLabels();
		table.pack();
		stage.setKeyboardFocus(scroll);
		stage.setScrollFocus(scroll);
	}

	private void updateLabels() {
		sortByS.setText(SORT_BY_SYLLABARY + getIndicator(DisplayRecord.SortBy.Syllabary));
		sortByL.setText(SORT_BY_LATIN + getIndicator(DisplayRecord.SortBy.Latin));
		sortByD.setText(SORT_BY_ENGLISH + getIndicator(DisplayRecord.SortBy.Definition));
	}

}
