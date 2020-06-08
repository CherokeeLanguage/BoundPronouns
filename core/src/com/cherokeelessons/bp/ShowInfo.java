package com.cherokeelessons.bp;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.cherokeelessons.bp.BoundPronouns.Font;

public class ShowInfo extends ChildScreen {

	private final Skin skin;
	private ScrollPane scroll;

	private Table table;
	private final Table container;

	private final Runnable initView = new Runnable() {
		@Override
		public void run() {
			final LabelStyle ls = new LabelStyle(skin.get("default", LabelStyle.class));
			ls.font = game.getFont(Font.SerifSmall);
			ls.background = null;

			container.row();
			final TextButtonStyle bls = new TextButtonStyle(skin.get("default", TextButtonStyle.class));
			bls.font = game.getFont(Font.SerifMedium);
			final TextButton back = new TextButton(BoundPronouns.BACK_ARROW, bls);
			container.add(back).left().width(BoundPronouns.BACK_WIDTH);
			back.addListener(exit);

			table = new Table(skin);

			scroll = new ScrollPane(table, skin);
			scroll.setColor(Color.DARK_GRAY);
			scroll.setFadeScrollBars(false);
			scroll.setSmoothScrolling(true);

			String text = Gdx.files.internal("text/instructions.txt").readString("UTF-8");
			text += "\n\n";
			final Label label = new Label(text, ls);
			label.setWrap(true);

			table.row();
			table.add(label).expand().fill().left().padLeft(20).padRight(20);

			container.row();
			container.add(scroll).expand().fill();
			stage.setKeyboardFocus(scroll);
			stage.setScrollFocus(scroll);
		}
	};

	public ShowInfo(final BoundPronouns game, final MainScreen mainScreen) {
		super(game, mainScreen);

		skin = new Skin(Gdx.files.internal(BoundPronouns.SKIN));

		container = new Table(skin);
		stage.addActor(container);

		container.setBackground(d());
		container.setFillParent(true);
		container.addAction(Actions.delay(.1f, Actions.run(initView)));
	}

	private TiledDrawable d() {
		final Texture texture = game.manager.get(BoundPronouns.IMG_MAYAN, Texture.class);
		return new TiledDrawable(new TextureRegion(texture));
	}

	@Override
	public void dispose() {
		skin.dispose();
		super.dispose();
	}

}
