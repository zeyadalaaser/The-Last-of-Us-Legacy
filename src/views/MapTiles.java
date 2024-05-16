package views;

import java.awt.Point;
import javafx.geometry.Insets;

import engine.Game;
import javafx.geometry.HPos;
import javafx.scene.image.*;
import javafx.scene.layout.GridPane;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.*;
import javafx.scene.paint.Color;
import model.world.*;
import model.characters.Character;
import model.characters.*;

public class MapTiles extends GridPane {

	private final static double tileWidth = 52.6;
	private static MapCell[][] map;

	public MapTiles() {
		super();
		this.setPadding(new Insets(10));
		this.setHgap(0);
		this.setVgap(0);
		map = new MapCell[15][15];
	}
	
	private static Point transform(int x, int y) // transforms game coordinates to gridpane coords
	{
		return new Point(y, 14 - x);
	}

	public void add(ImageView image, int x, int y) {
		image.setPreserveRatio(false);
		image.setFitWidth(tileWidth);
		image.setFitHeight(tileWidth);
		image.setSmooth(true);
		image.setCache(true);
		GridPane.setHalignment(image, HPos.CENTER);

		Point point = transform(x, y);
		super.add(image, point.x, point.y);
	}
	
	public void add(Image tileImage, int x, int y, Image image) {
		ImageView characterImage = new ImageView(image);
		ImageView tile = new ImageView(tileImage);

		tile.setPreserveRatio(false);
		tile.setFitWidth(tileWidth);
		tile.setFitHeight(tileWidth);
		tile.setSmooth(true);
		tile.setCache(true);
		
		characterImage.setPreserveRatio(false);
		characterImage.setFitWidth(tileWidth);
		characterImage.setFitHeight(tileWidth);
		characterImage.setSmooth(true);
		characterImage.setCache(true);
		GridPane.setHalignment(characterImage, HPos.CENTER);

		Point point = transform(x, y);
		super.add(tile, point.x, point.y);
		super.add(characterImage, point.x, point.y);
		
		characterImage.setOnMouseEntered(e -> 
		{
			Cell cell = Game.map[x][y];
			
			if (!(cell instanceof CharacterCell) || ((CharacterCell)cell).getCharacter() == null)
				return;
			
			
			Character character = ((CharacterCell)cell).getCharacter();
			String tooltipText;
			
			if (character instanceof Zombie)
				tooltipText = "HP: "+ character.getCurrentHp() + "\n" + "Damage: " + character.getAttackDmg();
			else
				tooltipText = character.getName();
			
			Tooltip tooltip = new Tooltip(tooltipText);
			tooltip.setStyle("-fx-font-size: 16px");
            Tooltip.install(characterImage, tooltip);
            
		});
		
//		characterImage.setOnMouseExited(e -> 
//		{
//			Tooltip.install(characterImage, null);
//		});
		
		characterImage.setOnMouseClicked(e -> {
			Cell cell = Game.map[x][y];
			if (cell instanceof CollectibleCell || cell instanceof TrapCell)
				return;
			
			Character character = ((CharacterCell)cell).getCharacter();
			if (character instanceof Hero && !Main.waitingForTarget)
			{
				// remove el shadow around the old hero's target (if the hero has a target)
				
				if (Main.playingHero != null && Main.playingHero.getTarget() != null)
				{
					Point oldTargetLocation = Main.playingHero.getTarget().getLocation();
					MapCell oldTargetCell = Main.tiles.getCell(oldTargetLocation.x, 
							oldTargetLocation.y);
					oldTargetCell.getImageAboveTile().setEffect(null);
				}
				
				Main.playingHero = (Hero)character;
				Main.updateDetails();
				// add shadow around new hero's target (if the hero has a target)
				
				if (character.getTarget() != null)
				{
					Point targetLocation = Main.playingHero.getTarget().getLocation();
					MapCell targetCell = Main.tiles.getCell(targetLocation.x, targetLocation.y);
					targetCell.getImageAboveTile().setEffect(new DropShadow(6, Color.RED));
				}
				
				characterImage.requestFocus();
			}
			else if (character != null && Main.waitingForTarget) // in case of medic heal
			{
				Main.playingHero.setTarget(character);
				Main.waitingForTarget = false;
				Main.medicHeal();
			}
			else if (character instanceof Zombie)
			{
				// remove shadow around old target
				
				if (Main.playingHero.getTarget() != null)
				{
					Point oldTargetLocation = Main.playingHero.getTarget().getLocation();
					MapCell oldTargetCell = Main.tiles.getCell(oldTargetLocation.x, 
							oldTargetLocation.y);
					oldTargetCell.getImageAboveTile().setEffect(null);
				}
				
				Main.playingHero.setTarget(character);
				characterImage.setEffect(new DropShadow(6, Color.RED));
			}
		});

		characterImage.focusedProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue) {
				characterImage.setEffect(new DropShadow(6, Color.AQUA));
			} else {
				characterImage.setEffect(null);
			}
		});

		MapCell cell = new MapCell(tile, characterImage);
		map[x][y] = cell;
	}

	public MapCell getCell(int x, int y) {
		return map[x][y];
	}
}
