package views;

import javafx.scene.image.*;

public class MapCell {
	
	public final static Image visibleCell = new Image("/resources/grass2.png");
	public final static Image invisibleCell = new Image("/resources/grass.png");
	
	private ImageView tile;
	private ImageView imageAboveTile;
	
	public MapCell(ImageView tile, ImageView imageAboveTile)
	{
		this.tile = tile;
		this.imageAboveTile = imageAboveTile;
	}

	public ImageView getTile() {
		return tile;
	}

	public ImageView getImageAboveTile() {
		return imageAboveTile;
	}
	
	public void setVisibility(boolean value)
	{
		if (value)
			tile.setImage(visibleCell);
		else
			tile.setImage(invisibleCell);
		imageAboveTile.setVisible(value);
	}
}
