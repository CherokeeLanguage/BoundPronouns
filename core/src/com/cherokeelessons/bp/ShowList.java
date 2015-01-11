package com.cherokeelessons.bp;

import java.util.List;

import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.FitViewport;

public class ShowList implements Screen {

	private static final String SORT_BY_ENGLISH = "Sort by English";
	private static final String SORT_BY_LATIN = "Sort by Latin";
	private static final String SORT_BY_SYLLABARY = "Sort by Syllabary";
	private final BoundPronouns game;
	private final FitViewport viewport;
	private final Stage stage;

	private ClickListener die = new ClickListener() {
		public boolean touchDown(InputEvent event, float x, float y,
				int pointer, int button) {
			game.setScreen(caller);
			dispose();
			return true;
		};
	};
	private final Screen caller;
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
				nextOrdinal %= DisplayRecord.SortOrder.values().length;
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
	private Label sortByS;
	private Label sortByL;
	private Label sortByD;

	public ShowList(BoundPronouns game, Screen callingScreen,
			List<CSVRecord> records) {
		Texture texture = game.manager.get(BoundPronouns.IMG_PAPER1,
				Texture.class);
		Drawable d = new TextureRegionDrawable(new TextureRegion(texture));
		
		this.caller = callingScreen;
		this.game = game;
		stage = new Stage();
		viewport = new FitViewport(1280, 720, stage.getCamera());
		viewport.update(1280, 720, true);
		stage.setViewport(viewport);

		LabelStyle ls = new LabelStyle(game.manager.get("font36.ttf",
				BitmapFont.class), Color.BLUE);

		Table container = new Table();
		stage.addActor(container);
		container.setBackground(d);
		container.setFillParent(true);
		container.setDebug(true, true);

		container.row();
		Label back = new Label(BoundPronouns.BACK_ARROW, new LabelStyle(ls));
		container.add(back);
		back.addListener(die);
		sortByS = new Label(SORT_BY_SYLLABARY, new LabelStyle(ls));
		sortByS.addListener(list_sortByS);
		container.add(sortByS);
		sortByL = new Label(SORT_BY_LATIN, new LabelStyle(ls));
		sortByL.addListener(list_sortByL);
		container.add(sortByL);
		sortByD = new Label(SORT_BY_ENGLISH, new LabelStyle(ls));
		sortByD.addListener(list_sortByD);
		int c = container.add(sortByD).getColumn();

		table = new Table();
		
		table.setBackground(d);
		ScrollPane scroll = new ScrollPane(table);
		scroll.setFadeScrollBars(false);
		scroll.setSmoothScrolling(true);
		scroll.setScrollBarPositions(true, true);

		container.row().getColumn();
		container.add(scroll).expand().fill().colspan(c + 1);

		String prevLatin = "";
		String prevChr = "";
		for (CSVRecord record : records) {
			String chr = record.get(0);
			if (chr.startsWith("#")) {
				continue;
			}
			String latin = record.get(1);
			String defin = record.get(2);
			if (StringUtils.isBlank(latin)) {
				latin = prevLatin;
			}
			if (StringUtils.isBlank(chr)) {
				chr = prevChr;
			}
			DisplayRecord dr = new DisplayRecord();
			Label actor;

			ls.fontColor = Color.DARK_GRAY;
			actor = new Label(chr, new LabelStyle(ls));
			dr.syllabary = actor;

			ls.fontColor = Color.DARK_GRAY;
			actor = new Label(latin, new LabelStyle(ls));
			dr.latin = actor;

			ls.fontColor = Color.DARK_GRAY;
			actor = new Label(defin, new LabelStyle(ls));
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
			cell_syll.align(Align.left).padLeft(30).padRight(15).expandX();
			Cell<Label> cell_latin = table.add(rec.latin);
			cell_latin.align(Align.left).padRight(15).expandX();
			Cell<Label> cell_def = table.add(rec.definition);
			cell_def.align(Align.left).padRight(30).padBottom(5).expandX();
			int span = cell_def.getColumn() + 1;
			if (greenbar) {
				table.row();
				table.add(new Label(" ", rec.syllabary.getStyle())).colspan(
						span);
			}
		}
		updateLabels();
	}

	private void updateLabels() {		
		String tmp;
		
		sortByS.setText(SORT_BY_SYLLABARY+getIndicator(DisplayRecord.SortBy.Syllabary));
		sortByL.setText(SORT_BY_LATIN+getIndicator(DisplayRecord.SortBy.Latin));
		sortByD.setText(SORT_BY_ENGLISH+getIndicator(DisplayRecord.SortBy.Definition));
	}

	private String getIndicator(com.cherokeelessons.bp.ShowList.DisplayRecord.SortBy by) {
		if (!DisplayRecord.by.equals(by)){
			return "";
		}
		switch (DisplayRecord.order) {
		case Ascending:
			return " "+BoundPronouns.TRIANGLE_ASC;
		case Descending:
			return " "+BoundPronouns.TRIANGLE_DESC;
		case SplitAscending:
			return " "+BoundPronouns.TRIANGLE_ASC+BoundPronouns.DIAMOND;
		case SplitDescending:
			return " "+BoundPronouns.TRIANGLE_DESC+BoundPronouns.DIAMOND;
		}
		return "";
	}

	@Override
	public void show() {
		Gdx.input.setInputProcessor(stage);
	}

	@Override
	public void render(float delta) {
		stage.act();
		Gdx.gl.glClearColor(1, 1, 1, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		stage.draw();
	}

	@Override
	public void resize(int width, int height) {
		stage.getViewport().update(width, height);
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub

	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub

	}

	@Override
	public void hide() {
		Gdx.input.setInputProcessor(null);
	}

	@Override
	public void dispose() {
		stage.dispose();
		drecs.clear();
	}

}
