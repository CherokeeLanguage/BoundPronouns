package com.cherokeelessons.bp;

import java.util.List;

import org.apache.commons.csv.CSVRecord;
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
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.utils.Array;

public class ShowList extends ChildScreen {

	private static final String SORT_BY_ENGLISH = "Sort by English";
	private static final String SORT_BY_LATIN = "Sort by Latin";
	private static final String SORT_BY_SYLLABARY = "Sort by Syllabary";
	private final Array<DisplayRecord> drecs = new Array<>();

	private static class DisplayRecord implements Comparable<DisplayRecord> {
		public static enum SortBy {
			Syllabary, Latin, Definition;
		}

		public static enum SortOrder {
			Ascending, Descending, SplitAscending, SplitDescending;
		}

		private static SortBy by = SortBy.Syllabary;
		private static SortOrder order = SortOrder.Ascending;

		public static void setSortBy(SortBy by) {
			if (by == null) {
				return;
			}
			if (DisplayRecord.by.equals(by)) {
				int nextOrdinal = DisplayRecord.order.ordinal() + 1;
				if (!by.equals(DisplayRecord.SortBy.Definition)){
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

		public static void setSortSubOrder(SortOrder order) {
			if (order == null) {
				return;
			}
			DisplayRecord.order = order;
		}

		public Label syllabary;
		public Label latin;
		public Label definition;

		private String sortKey() {
			switch (by) {
			case Definition:
				String string = definition.getText().toString() + "|"
						+ syllabary.getText().toString() + "|"
						+ latin.getText().toString();
				string = cleanup(string);
				return string.toLowerCase();
			case Latin:
				String string2 = latin.getText().toString() + "|"
						+ definition.getText().toString() + "|"
						+ syllabary.getText().toString();
				string2 = cleanup(string2);
				return string2.toLowerCase();
			case Syllabary:
				String string3 = syllabary.getText().toString() + "|"
						+ latin.getText().toString() + "|"
						+ definition.getText().toString();
				string3 = cleanup(string3);
				return string3.toLowerCase();
			}
			return "";
		}

		private String cleanup(String string) {
			string = string.replace(BoundPronouns.UNDERDOT, "");
			string = string.replace(BoundPronouns.UNDERX, "");
			string = StringUtils.replaceChars(string, "ạẹịọụṿẠẸỊỌỤṾ",
					"aeiouvAEIOUV");
			string = string.replaceAll("[¹²³⁴]", "");
			string = StringUtils.normalizeSpace(string);
			string = StringUtils.strip(string);
			return string;
		}

		@Override
		public int compareTo(DisplayRecord o) {
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
			}
			return sortKey().compareTo(o.sortKey());
		}

		public int splitCompare(DisplayRecord o) {
			String[] x1 = sortKey().split("\\|");
			String[] x2 = o.sortKey().split("\\|");
			StringBuilder sb1 = new StringBuilder();
			StringBuilder sb2 = new StringBuilder();
			for (String x : x1) {
				sb1.append(StringUtils.substringAfter(x, "+"));
				sb1.append("+");
				sb1.append(StringUtils.substringBefore(x, "+"));
				sb1.append("|");
			}
			for (String x : x2) {
				sb2.append(StringUtils.substringAfter(x, "+"));
				sb2.append("+");
				sb2.append(StringUtils.substringBefore(x, "+"));
				sb2.append("|");
			}
			return sb1.toString().compareTo(sb2.toString());
		}
	}

	private final Table table;
	private ClickListener list_sortByD = new ClickListener() {
		@Override
		public boolean touchDown(InputEvent event, float x, float y,
				int pointer, int button) {
			DisplayRecord.setSortBy(DisplayRecord.SortBy.Definition);
			drecs.sort();
			populateList();
			return true;
		}
	};
	private ClickListener list_sortByL = new ClickListener() {
		public boolean touchDown(InputEvent event, float x, float y,
				int pointer, int button) {
			DisplayRecord.setSortBy(DisplayRecord.SortBy.Latin);
			drecs.sort();
			populateList();
			return true;
		};
	};
	private ClickListener list_sortByS = new ClickListener() {
		public boolean touchDown(InputEvent event, float x, float y,
				int pointer, int button) {
			DisplayRecord.setSortBy(DisplayRecord.SortBy.Syllabary);
			drecs.sort();
			populateList();
			return true;
		};
	};
	private TextButton sortByS;
	private TextButton sortByL;
	private TextButton sortByD;
	private ScrollPane scroll;
	private final Skin skin;
	private final Table container;
	
	public ShowList(final BoundPronouns game, Screen callingScreen,
			final List<CSVRecord> records) {
		super(game, callingScreen);
		
		skin = game.manager.get(BoundPronouns.SKIN, Skin.class);
		table = new Table();
		container = new Table(skin);
		stage.addActor(container);
		Texture texture = game.manager.get(BoundPronouns.IMG_MAYAN,
				Texture.class);
		TiledDrawable d = new TiledDrawable(new TextureRegion(texture));
		container.setBackground(d);
		container.setFillParent(true);
		Gdx.app.postRunnable(new Runnable() {
			@Override
			public void run() {
				initialPopulate(game, records);
			}
		});
		
	}

	public void initialPopulate(BoundPronouns game, List<CSVRecord> records) {
		Texture texture = game.manager.get(BoundPronouns.IMG_MAYAN,
				Texture.class);
		TiledDrawable d = new TiledDrawable(new TextureRegion(texture));
		BitmapFont f36_base = game.manager.get("sans36.ttf", BitmapFont.class);
		BitmapFont f36_serif = game.manager.get("serif36.ttf", BitmapFont.class);
		BitmapFont f36 = new BitmapFont(f36_base.getData(), f36_base.getRegions(), true);
		f36.setMarkupEnabled(true);
		
		TextButtonStyle bstyle = new TextButtonStyle(skin.get("default", TextButtonStyle.class));
		bstyle.font=f36;
		container.row();
		TextButtonStyle bls=new TextButtonStyle(bstyle);
		bls.fontColor=Color.BLUE;
		TextButton back = new TextButton(BoundPronouns.BACK_ARROW, bls);
		container.add(back).center().fill().width(BoundPronouns.BACK_WIDTH);
		back.addListener(exit);

		sortByS = new TextButton(SORT_BY_SYLLABARY, bstyle);
		sortByS.addListener(list_sortByS);
		container.add(sortByS).center().fill().expand();
		
		sortByL = new TextButton(SORT_BY_LATIN, bstyle);
		sortByL.addListener(list_sortByL);
		container.add(sortByL).center().fill().expand();
		
		sortByD = new TextButton(SORT_BY_ENGLISH, bstyle);
		sortByD.addListener(list_sortByD);

		int c = container.add(sortByD).center().fill().expand().getColumn();

		table.setBackground(d);

		scroll = new ScrollPane(table, skin);
		scroll.setColor(Color.DARK_GRAY);
		scroll.setFadeScrollBars(false);
		scroll.setSmoothScrolling(true);		
		container.row();
		container.add(scroll).expand().fill().colspan(c + 1);		
		
		LabelStyle ls = new LabelStyle(skin.get("default", LabelStyle.class));
		ls.font=f36_base;
		ls.background=null;
		
		String prevLatin = "";
		String prevChr = "";
		for (CSVRecord record : records) {
			String vtmode=record.get(0);
			if (StringUtils.isBlank(vtmode)){
				continue;
			}
			String chr = record.get(1);
			if (chr.startsWith("#")) {
				continue;
			}
			String latin = record.get(2);
			String defin = record.get(3)+" + "+record.get(4);
			if (StringUtils.isBlank(record.get(3))){
				String tmp = record.get(4);
				passive:{
					if (tmp.equalsIgnoreCase("he")){
						defin = tmp+" (was being)";
						break passive;
					}
					if (tmp.equalsIgnoreCase("i")){
						defin = tmp+" (was being)";
						break passive;
					}
					defin = tmp+" (were being)";
					break passive;
				}				
			}
			if (StringUtils.isBlank(latin)) {
				latin = prevLatin;
			}
			if (StringUtils.isBlank(chr)) {
				chr = prevChr;
			}
			DisplayRecord dr = new DisplayRecord();
			
			Label actor;

			actor = new Label(chr, ls);
			dr.syllabary = actor;

			actor = new Label(latin, ls);
			dr.latin = actor;

			actor = new Label(defin, ls);
			dr.definition = actor;

			drecs.add(dr);

			prevLatin = latin;
			prevChr = chr;
		}
		DisplayRecord.setSortBy(DisplayRecord.SortBy.Syllabary);
		DisplayRecord.setSortSubOrder(DisplayRecord.SortOrder.Ascending);
		drecs.sort();
		populateList();	
	}

	private void populateList() {
		int ix = 0;
		table.clear();
		for (DisplayRecord rec : drecs) {
			ix++;
			boolean greenbar = ((ix % 3) == 0);
			table.row();
			Cell<Label> cell_syll = table.add(rec.syllabary);
			cell_syll.align(Align.left).padLeft(12).padRight(6).padBottom(5).expandX();
			Cell<Label> cell_latin = table.add(rec.latin);
			cell_latin.align(Align.left).padRight(6).padBottom(5).expandX();
			Cell<Label> cell_def = table.add(rec.definition);
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
		sortByS.setText(SORT_BY_SYLLABARY
				+ getIndicator(DisplayRecord.SortBy.Syllabary));
		sortByL.setText(SORT_BY_LATIN
				+ getIndicator(DisplayRecord.SortBy.Latin));
		sortByD.setText(SORT_BY_ENGLISH
				+ getIndicator(DisplayRecord.SortBy.Definition));
	}

	private String getIndicator(
			com.cherokeelessons.bp.ShowList.DisplayRecord.SortBy by) {
		final String trans = "[#00000000]";
		if (!DisplayRecord.by.equals(by)) {
			return trans + " " + BoundPronouns.TRIANGLE_ASC + trans + BoundPronouns.DIAMOND + "[]";
		}
		switch (DisplayRecord.order) {
		case Ascending:
			return " " + BoundPronouns.TRIANGLE_ASC + trans + BoundPronouns.DIAMOND + "[]";
		case Descending:
			return " " + BoundPronouns.TRIANGLE_DESC + trans + BoundPronouns.DIAMOND + "[]";
		case SplitAscending:
			return " " + BoundPronouns.TRIANGLE_ASC + BoundPronouns.DIAMOND + "[]";
		case SplitDescending:
			return " " + BoundPronouns.TRIANGLE_DESC + BoundPronouns.DIAMOND + "[]";
		}
		return trans + " " + BoundPronouns.TRIANGLE_ASC + trans + BoundPronouns.DIAMOND + "[]";
	}


	@Override
	public void dispose() {
		drecs.clear();
		super.dispose();
	}

}
