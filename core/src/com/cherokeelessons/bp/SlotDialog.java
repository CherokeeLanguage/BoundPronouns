package com.cherokeelessons.bp;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.utils.ObjectMap;

public class SlotDialog extends Dialog {

	public SlotDialog(String title, Skin skin, BoundPronouns game, BitmapFont tfont) {
		super(title, skin);
		
		WindowStyle ws = new WindowStyle(getStyle());
		BitmapFont f54 = tfont;
		
		Texture background = game.manager.get(BoundPronouns.IMG_MAYAN, Texture.class);
		TextureRegion region = new TextureRegion(background);
		TiledDrawable tiled = new TiledDrawable(region);
		tiled.setMinHeight(0);		
		tiled.setTopHeight(f54.getCapHeight()+20);
		
		ws.titleFont = f54;
		ws.background=tiled;
		
		setStyle(ws);
		
		getContentTable().addListener(new ChangeListener() {
			public void changed (ChangeEvent event, Actor actor) {
				if (!values.containsKey(actor)) return;
				while (actor.getParent() != getContentTable())
					actor = actor.getParent();
				result(values.get(actor));
				hide();				
			}
		});
	}
	
	private ObjectMap<Actor, Object> values = new ObjectMap<>();
	
	public Dialog text(TextButton tbutton, Object object) {
		Table contentTable = getContentTable();
		values.put(tbutton, object);
		contentTable.row();
		contentTable.add(tbutton).pad(0).expand().fill();
		return this;
	}
}
