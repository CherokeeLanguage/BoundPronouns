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
import com.badlogic.gdx.scenes.scene2d.EventListener;
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

	private final BoundPronouns game;
	private final FitViewport viewport;
	private final Stage stage;

	private List<CSVRecord> records;
	private ClickListener die = new ClickListener() {
		public boolean touchDown(InputEvent event, float x, float y,
				int pointer, int button) {
			game.setScreen(caller);
			dispose();
			return true;
		};
	};
	private final Screen caller;
	private final Array<DisplayRecord> drecs = new Array(); 
	private static class DisplayRecord implements Comparable<DisplayRecord> {
		public static enum SortBy {
			Syllabary, Latin, Definition;
		}

		private static SortBy by = SortBy.Syllabary;

		public static void setSortBy(SortBy by) {
			if (by==null) {
				return;
			}
			DisplayRecord.by = by;
		}

		public Label syllabary;
		public Label latin;
		public Label definition;

		private String sortKey() {
			switch (by) {
			case Definition:
				return definition.getText().toString()+syllabary.getText().toString()+latin.getText().toString();
			case Latin:
				return latin.getText().toString()+definition.getText().toString()+syllabary.getText().toString();
			case Syllabary:
				return syllabary.getText().toString()+latin.getText().toString()+definition.getText().toString();
			}
			return "";
		}

		@Override
		public int compareTo(DisplayRecord o) {
			if (o==null) {
				return 1;
			}
			return sortKey().compareTo(o.sortKey());
		}
	}

	private final Table table;
	private ClickListener list_sortByD=new ClickListener(){
		@Override
		public boolean touchDown(InputEvent event, float x, float y,
				int pointer, int button) {
			DisplayRecord.setSortBy(DisplayRecord.SortBy.Definition);
			drecs.sort();
			populateList();
			return true;
		}
	};
	private ClickListener list_sortByL=new ClickListener(){
		public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
			DisplayRecord.setSortBy(DisplayRecord.SortBy.Latin);
			drecs.sort();
			populateList();
			return true;
		};
	};
	private ClickListener list_sortByS=new ClickListener(){
		public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
			DisplayRecord.setSortBy(DisplayRecord.SortBy.Syllabary);
			drecs.sort();
			populateList();
			return true;
		};
	};
	
	public ShowList(BoundPronouns game, Screen callingScreen,
			List<CSVRecord> records) {
		this.caller = callingScreen;
		this.game = game;
		stage = new Stage();
		viewport = new FitViewport(1280, 720, stage.getCamera());
		viewport.update(1280, 720, true);
		stage.setViewport(viewport);
		this.records = records;

		LabelStyle ls = new LabelStyle(game.manager.get("font36.ttf",
				BitmapFont.class), Color.BLUE);

		Table container = new Table();
		stage.addActor(container);
		container.setFillParent(true);
		container.setDebug(true, true);

		container.row();
		Label back = new Label("<- BACK", new LabelStyle(ls));
		container.add(back);
		back.addListener(die);
		Label sortByS = new Label("Sort by Syllabary", new LabelStyle(ls));
		sortByS.addListener(list_sortByS);
		container.add(sortByS);
		Label sortByL = new Label("Sort by Latin", new LabelStyle(ls));
		sortByL.addListener(list_sortByL);
		container.add(sortByL);
		Label sortByD = new Label("Sort by English", new LabelStyle(ls));
		sortByD.addListener(list_sortByD);
		int c = container.add(sortByD).getColumn();

		table = new Table();
		Texture texture = game.manager.get(BoundPronouns.IMG_PAPER1, Texture.class);
		Drawable d = new TextureRegionDrawable(new TextureRegion(texture));
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
			dr.syllabary=actor;
			
			ls.fontColor = Color.DARK_GRAY;
			actor = new Label(latin, new LabelStyle(ls));
			dr.latin=actor;
			
			ls.fontColor = Color.DARK_GRAY;
			actor = new Label(defin, new LabelStyle(ls));
			dr.definition=actor;
			
			drecs.add(dr);
			
			prevLatin = latin;
			prevChr = chr;
		}
		DisplayRecord.setSortBy(DisplayRecord.SortBy.Syllabary);
		drecs.sort();
		populateList();
	}

	private void populateList() {
		int ix=0;
		table.clear();
		for (DisplayRecord rec: drecs) {
			ix++;
			boolean greenbar = ((ix%3)==0);
			table.row();
			Cell<Label> cell_syll = table.add(rec.syllabary);
			cell_syll.align(Align.left).padLeft(30).padRight(15).expandX();
			Cell<Label> cell_latin = table.add(rec.latin);
			cell_latin.align(Align.left).padRight(15).expandX();
			Cell<Label> cell_def = table.add(rec.definition);
			cell_def.align(Align.left).padRight(30).padBottom(5).expandX();
			int span=cell_def.getColumn()+1;
			if (greenbar) {
				table.row();
				table.add(new Label(" ", rec.syllabary.getStyle())).colspan(span);
			}
		}	
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
		records.clear();
	}

}
