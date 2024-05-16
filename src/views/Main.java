package views;

import engine.Game;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.*;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.stage.*;
import javafx.util.Duration;
import model.characters.*;
import model.collectibles.*;
import model.world.*;
import javafx.scene.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import java.util.Dictionary;
import java.util.Hashtable;
import java.awt.Point;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.image.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;

public class Main extends Application {

	public static Image cursor = new Image("/resources/cursor.png");
	public static Image hoverCursor = new Image("/resources/zombiehand.png");
	public static Image zombie = new Image("/resources/zombie.png");
	public static boolean herocliked = false;

	public static MapTiles tiles;
	public static Hero playingHero;
	public static boolean waitingForTarget;
	public static Stage stage;
	public static Label message;
	public static boolean gameOver = false;

	public static void adjustVisibility() {
		for (int i = 0; i < 15; i++) 
			for (int j = 0; j < 15; j++)
				tiles.getCell(i, j).setVisibility(Game.map[i][j].isVisible());
	}

	public static void move(Direction direction) throws Exception {
		if (playingHero == null) {
			showMessage("\nPlease choose a hero.");
			return;
		}

		int oldHp = playingHero.getCurrentHp();
		Point oldLocation = playingHero.getLocation();

		playingHero.move(direction);
		updateDetails();

		Point newLocation = playingHero.getLocation();
		int newHp = playingHero.getCurrentHp();

		MapCell oldCell = tiles.getCell(oldLocation.x, oldLocation.y);
		Image oldImage = oldCell.getImageAboveTile().getImage();
		oldCell.getImageAboveTile().setImage(null);

		MapCell newCell = tiles.getCell(newLocation.x, newLocation.y);
		newCell.getImageAboveTile().setImage(oldImage);
		newCell.getImageAboveTile().requestFocus();

		if (oldHp != newHp) {
			checkDeadCharacters();
			showMessage("\nYou ran into a trap cell.");
			animateFire(newLocation.x, newLocation.y);
		}

		adjustVisibility();
	}

	public static void startScene(Scene s) throws Exception {
		showMessage("\nSelect a hero to play.");

		Game.startGame(playingHero);
		String heroName = playingHero.getName();

		playingHero = null;
		updateDetails();

		stage.setFullScreen(true);
		stage.setMaximized(true);
		stage.setTitle("THE LAST OF US");

		tiles = new MapTiles();
		tiles.setAlignment(Pos.CENTER);

		Image aceFruit = new Image("/resources/acefruit.png");
		Image vaccine = new Image("/resources/vaccine.png");

		for (int i = 0; i < 15; i++)
			for (int j = 0; j < 15; j++) {
				Cell cell = Game.map[i][j];
				boolean isCharacterCell = cell instanceof CharacterCell;
				boolean isCollectibleCell = cell instanceof CollectibleCell;

				if (isCharacterCell && ((CharacterCell) cell).getCharacter() instanceof Zombie)
					tiles.add(MapCell.invisibleCell, i, j, zombie);
				else if (isCharacterCell && ((CharacterCell) cell).getCharacter() == null)
					tiles.add(MapCell.invisibleCell, i, j, null);
				else if (isCollectibleCell && ((CollectibleCell) cell).getCollectible() instanceof Supply)
					tiles.add(MapCell.invisibleCell, i, j, aceFruit);
				else if (isCollectibleCell && ((CollectibleCell) cell).getCollectible() instanceof Vaccine)
					tiles.add(MapCell.invisibleCell, i, j, vaccine);
				else // trap cell or 0,0
					tiles.add(MapCell.invisibleCell, i, j, null);

				tiles.getCell(i, j).setVisibility(false);
			}

		tiles.getCell(0, 0).getImageAboveTile().setImage(new Image("/resources/heroes/" + heroName + ".png"));
		adjustVisibility();
		// tiles.getCell(0, 0).getImageAboveTile().requestFocus();

		for (Node node : ((BorderPane) s.getRoot()).getChildren()) {
			if (node instanceof Pane)
				for (Node children : ((Pane) node).getChildren())
					children.setFocusTraversable(false);

			node.setFocusTraversable(false);
		}

		((BorderPane) s.getRoot()).setCenter(tiles);
		s.setOnKeyPressed(keyEvent -> {
			if (gameOver)
				System.exit(0);

			try {
				switch (keyEvent.getCode()) {
				case UP:
					if (waitingForTarget)
						waitingForTarget = false;

					move(Direction.UP);
					break;
				case DOWN:
					if (waitingForTarget)
						waitingForTarget = false;

					move(Direction.DOWN);
					break;
				case LEFT:
					if (waitingForTarget)
						waitingForTarget = false;

					move(Direction.LEFT);
					break;
				case RIGHT:
					if (waitingForTarget)
						waitingForTarget = false;

					move(Direction.RIGHT);
					break;
				case A:
					if (waitingForTarget)
						waitingForTarget = false;

					if (playingHero == null) {
						showMessage("\nPlease choose a hero.");
						return;
					}

					playingHero.attack();
					updateDetails();

					// System.out.println("Zombie hp: " + playingHero.getTarget().getCurrentHp());
					// System.out.println("My hp: " + playingHero.getCurrentHp());
					if (playingHero.getTarget().getCurrentHp() == 0) {
						// get new zombie spawned and add it to map
						checkNewZombies();
						checkDeadCharacters();
						// removeFromMap(playingHero.getTarget());
						playingHero.setTarget(null);

					}

					if (playingHero.getCurrentHp() == 0) {
						checkDeadCharacters();
						playingHero = null;
					}

					break;
				case C:
					if (playingHero == null) {
						showMessage("\nPlease choose a hero.");
						return;
					}

					if (waitingForTarget)
						waitingForTarget = false;

//					if (playingHero.getTarget() == null) {
//						System.out.println("e5tar zombie yahbal");
//						return;
//					}

					// playingHero.getVaccineInventory().add(new model.collectibles.Vaccine());
					playingHero.cure();
					updateDetails();
					Point targetLocation = playingHero.getTarget().getLocation();
					MapCell mapCell = tiles.getCell(targetLocation.x, targetLocation.y);

					CharacterCell characterCell = (CharacterCell) Game.map[targetLocation.x][targetLocation.y];

					mapCell.getImageAboveTile().setImage(
							new Image("/resources/heroes/" + characterCell.getCharacter().getName() + ".png"));
					mapCell.getImageAboveTile().setEffect(null);

					adjustVisibility();
					playingHero.setTarget(null);

					// cure
					break;
				case S:
					if (playingHero == null) {
						showMessage("\nPlease choose a hero.");
						return;
					}

					if (playingHero instanceof Medic) {
						if (playingHero.getTarget() == null) {
							showMessage("\nPlease select a hero to heal.");
							Main.waitingForTarget = true;
						} else {
							medicHeal();
						}
						return;
					}

					// playingHero.getSupplyInventory().add(new model.collectibles.Supply());
					playingHero.useSpecial();
					updateDetails();
					if (playingHero instanceof Explorer)
						adjustVisibility();

					if (waitingForTarget)
						waitingForTarget = false;

					break;
				case E:
					if (waitingForTarget)
						waitingForTarget = false;

					// (update labels?)
					Game.endTurn();
					checkNewZombies();
					checkDeadCharacters();
					adjustVisibility();
					updateDetails();

					if (Game.checkWin()) {
						showMessage("You win! Press any key to exit....");
						gameOver = true;
						// reset();
					} else if (Game.checkGameOver()) {
						showMessage("You lost! Press any key to exit....");
						gameOver = true;
						// reset();
					}
					break;
				default:
					break;
				}
			} catch (Exception ex) {
				if (ex.getMessage() == null)
					ex.printStackTrace();

				if (ex.getMessage() == null)
					ex.printStackTrace();

				if (ex.getMessage().contains("action point"))
					showError(actions);

				if (ex.getMessage().contains("supply"))
					showError(supplies);

				if (ex.getMessage().contains("vaccine"))
					showError(vaccines);

				showMessage("\n" + ex.getMessage());
			}
		});

//		stage.setScene(s);
//		stage.show();
	}

	public void start(Stage primaryStage) throws Exception {
		primaryStage.setFullScreenExitKeyCombination(javafx.scene.input.KeyCombination.NO_MATCH);
		InputStream f1 = getClass().getResourceAsStream("/resources/8bitOperatorPlus8-Bold.ttf");
		InputStream f2 = getClass().getResourceAsStream("/resources/8bitOperatorPlus8-Bold.ttf");
		InputStream f3 = getClass().getResourceAsStream("/resources/8bitOperatorPlus8-Bold.ttf");
		InputStream f4 = getClass().getResourceAsStream("/resources/8bitOperatorPlus8-Bold.ttf");
		InputStream f5 = getClass().getResourceAsStream("/resources/8bitOperatorPlus8-Bold.ttf");
		InputStream f6 = getClass().getResourceAsStream("/resources/8bitOperatorPlus8-Bold.ttf");

		Font f100 = Font.loadFont(f1, 100);
		Font f55 = Font.loadFont(f2, 55);
		Font f20 = Font.loadFont(f3, 20);
		Font f16 = Font.loadFont(f6, 16);
		Font f40 = Font.loadFont(f4, 40);
		Font f30 = Font.loadFont(f5, 30);

		Main.stage = primaryStage;
		Game.loadHeroes(getClass().getResourceAsStream("/resources/Heroes.csv"));

		primaryStage.getIcons().add(new Image("/resources/icon20.png"));
		primaryStage.setTitle("THE LAST OF US");

		// Making home page

		StackPane homeRoot = new StackPane();

		Image img = new Image("/resources/covernew.jpg");
		BackgroundImage bImg = new BackgroundImage(img, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
				BackgroundPosition.CENTER, BackgroundSize.DEFAULT);
		Background bGround = new Background(bImg);
		homeRoot.setBackground(bGround);
		Scene homeScene = createScene(homeRoot);

		Image img2 = new Image("/resources/test.png");
		BackgroundImage bImg2 = new BackgroundImage(img2, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
				BackgroundPosition.CENTER, BackgroundSize.DEFAULT);
		Background bGround2 = new Background(bImg2);

		Label startButton = new Label("   Start Game   ");
		zabbat(startButton, true);
		startButton.setFont(f100);
		startButton.setTranslateX(20);
		startButton.setTranslateY(270);
		homeRoot.getChildren().add(startButton);

		primaryStage.setFullScreen(true);
		primaryStage.setScene(homeScene);
		primaryStage.show();

		// Making a Instructions page

		GridPane instructionroot = new GridPane();
		instructionroot.setAlignment(Pos.CENTER_RIGHT);
		VBox stack6 = new VBox();
		stack6.setSpacing(15);
		// fonts

		Label m0 = new Label("Game Introduction");
		m0.setFont(f55);
		m0.setTextFill(Color.BROWN);
		Label m1 = new Label("The Last of Us is a single player survival game set in a zombie apocalyptic world");
		m1.setFont(f20);
		m1.setTextFill(Color.WHITE);
		Label m2 = new Label("The game is conducted in a turn based manner");
		m2.setFont(f20);
		m2.setTextFill(Color.WHITE);
		Label m3 = new Label("in which each hero receives a specific number of action points per turn");
		m3.setFont(f20);
		m3.setTextFill(Color.WHITE);
		Label m4 = new Label("which they can use to move, attack or cure zombies, or use special  actions");
		m4.setFont(f20);
		m4.setTextFill(Color.WHITE);
		Label m5 = new Label(
				"The player starts the game controlling only one hero,\nbut can gain additional heroes by curing zombies");
		m5.setFont(f20);
		m5.setTextFill(Color.WHITE);
		Label m6 = new Label("The objective of the game for the player is to survive as long as it takes in order to ");
		m6.setFont(f20);
		m6.setTextFill(Color.WHITE);
		Label m7 = new Label(
				"cure a sufficient number of zombies enough to build a community to survive the apocalypse.");
		m7.setFont(f20);
		m7.setTextFill(Color.WHITE);
		Label m8 = new Label("in order to win you have to collect all the vaccines in the map and ");
		m8.setFont(f20);
		m8.setTextFill(Color.WHITE);
		Label m9 = new Label("be in control of atleast 5 heroes you've obtained through curing if you collect");
		m9.setFont(f20);
		m9.setTextFill(Color.WHITE);
		Label m10 = new Label("all the vaccines on the map without being in control of atleast 5 heroes you lose.\n ");
		m10.setFont(f20);
		m10.setTextFill(Color.WHITE);

		Button skip = new Button("Skip");
		skip.setCursor(new ImageCursor(hoverCursor));
		skip.setTextFill(Color.WHITE);
		skip.setFont(f40);
		skip.setStyle("-fx-background-color: BROWN ;");

		stack6.getChildren().addAll(m0, m1, m2, m3, m4, m5, m6, m7, m8, m9, m10, skip);
		instructionroot.add(stack6, 0, 0);

		instructionroot.setBackground(bGround2);

		Scene instructionScene = createScene(instructionroot);

		startButton.setOnMouseClicked(e -> {
			primaryStage.setScene(instructionScene);
			primaryStage.setFullScreen(true);
		});

		// Making Select Heroes Page

		GridPane selectHeroesRoot = new GridPane();
		selectHeroesRoot.setAlignment(Pos.BASELINE_RIGHT);

		HBox firstRow = new HBox();
		HBox secondRow = new HBox();
		HBox third = new HBox();
		third.setSpacing(300);

		VBox stack = new VBox();

		Button letsbegin = new Button("Let's Begin!");
		letsbegin.setVisible(false);
		letsbegin.setCursor(new ImageCursor(hoverCursor));
		letsbegin.setTextFill(Color.WHITE);
		letsbegin.setStyle("-fx-background-color: BROWN ;");
		letsbegin.setFont(f40);

		Button backtohomepage = new Button("Back to Home page");
		backtohomepage.setCursor(new ImageCursor(hoverCursor));
		backtohomepage.setTextFill(Color.WHITE);
		backtohomepage.setFont(f40);
		backtohomepage.setStyle("-fx-background-color: BROWN ;");

		third.getChildren().addAll(backtohomepage, letsbegin);

		Label L1 = new Label("Select the Hero you\nwant to start with!");
		L1.setFont(f55);
		L1.setTextFill(Color.WHITE);
		Label L11 = new Label();
		L11.setFont(f55);
		L11.setTextFill(Color.WHITE);
		Label L2 = new Label();
		L2.setFont(f55);
		L2.setTextFill(Color.WHITE);
		Label L22 = new Label();
		L22.setFont(f55);
		L22.setTextFill(Color.WHITE);
		Label L3 = new Label();
		L3.setFont(f55);
		L3.setTextFill(Color.WHITE);
		Label L4 = new Label();
		L4.setFont(f55);
		L4.setTextFill(Color.WHITE);
		Label L5 = new Label();
		L5.setFont(f55);
		L5.setTextFill(Color.WHITE);

		for (int i = 0; i < Game.availableHeroes.size(); i++) {
			Screen screen = Screen.getPrimary();
			Rectangle2D bounds = screen.getVisualBounds();
			Hero h = Game.availableHeroes.get(i);
			Image image = new Image("/resources/" + h.getName() + ".jpg");
			ImageView view = new ImageView(image);
			// System.out.println(bounds.getWidth());
			// System.out.println(bounds.getHeight());
			view.setFitWidth(bounds.getWidth() * 0.13);
			view.setFitHeight(bounds.getHeight() * 0.243);

//			view.fitWidthProperty().bind(Bindings.divide(selectHeroesRoot.widthProperty(), 7.5));
//			view.fitHeightProperty().bind(Bindings.divide(selectHeroesRoot.heightProperty(), 4.5));

			Button heroButton = new Button();
			zabbat(heroButton, true);
			heroButton.setGraphic(view);
			heroButton.setPrefSize(150, 150);

			heroButton.setOnMouseEntered(e -> {
				if (!herocliked) {
					L2.setText("Name: " + h.getName());
					switch (h.getName()) {
					case "Joel Miller":
						L1.setText("Oh yeahh!");
						break;
					case "Ellie Williams":
						L1.setText("Correct Choice");
						break;
					case "Tess":
						L1.setText("That's Bold!");
						break;
					case "Riley Abel":
						L1.setText("Cute Choice!");
						break;
					case "Tommy Miller":
						L1.setText("Awesome!");
						break;
					case "Bill":
						L1.setText("So Cool!");
						break;
					case "David":
						L1.setText("Ohhh!");
						break;
					case "Henry Burell":
						L1.setText("Lets GO!");
						break;
					}

					L22.setText("Type: " + h.getClass().getSimpleName());
					L3.setText("Max Health points: " + Integer.toString(h.getMaxHp()));
					L4.setText("Attack Damage: " + Integer.toString(h.getAttackDmg()));
					L5.setText("Max Actions: " + Integer.toString(h.getMaxActions()) + "\n ");

				}
			});

			heroButton.setOnMouseExited(e -> {
				if (!herocliked) {
					L1.setText("Select the Hero you\nwant to start with!");
					L2.setText("");
					L22.setText("");
					L3.setText("");
					L4.setText("");
					L5.setText("");
				}

			});

			heroButton.setOnMouseClicked(e -> {
				for (Hero hero : Game.availableHeroes)
					if (hero.getName().equals(h.getName()))
						playingHero = hero;


				letsbegin.setVisible(true);

				L2.setText("Name: " + h.getName());
				L2.setTextFill(Color.CHOCOLATE);
				switch (h.getName()) {
				case "Joel Miller":
					L1.setText("Oh yeahh!");
					L1.setTextFill(Color.CHOCOLATE);
					break;
				case "Ellie Williams":
					L1.setText("Correct Choice");
					L1.setTextFill(Color.CHOCOLATE);
					break;
				case "Tess":
					L1.setText("That's Bold!");
					L1.setTextFill(Color.CHOCOLATE);
					break;
				case "Riley Abel":
					L1.setText("Cute Choice!");
					L1.setTextFill(Color.CHOCOLATE);
					break;
				case "Tommy Miller":
					L1.setText("Awesome!");
					L1.setTextFill(Color.CHOCOLATE);
					break;
				case "Bill":
					L1.setText("So Cool!");
					L1.setTextFill(Color.CHOCOLATE);
					break;
				case "David":
					L1.setText("Ohhh!");
					L1.setTextFill(Color.CHOCOLATE);
					break;
				case "Henry Burell":
					L1.setText("Lets GO!");
					L1.setTextFill(Color.CHOCOLATE);
					break;
				}
				L22.setText("Type: " + h.getClass().getSimpleName());
				L22.setTextFill(Color.CHOCOLATE);
				L3.setText("Max Health points: " + Integer.toString(h.getMaxHp()));
				L3.setTextFill(Color.CHOCOLATE);
				L4.setText("Attack Damage: " + Integer.toString(h.getAttackDmg()));
				L4.setTextFill(Color.CHOCOLATE);
				L5.setText("Max Actions: " + Integer.toString(h.getMaxActions()) + "\n ");
				L5.setTextFill(Color.CHOCOLATE);

				herocliked = true;

			});

			if (firstRow.getChildren().size() != 4)
				firstRow.getChildren().add(heroButton);
			else
				secondRow.getChildren().add(heroButton);

		}

		stack.getChildren().addAll(firstRow, secondRow, L1, L2, L22, L3, L4, L5, third);
		selectHeroesRoot.add(stack, 0, 0);

		Scene selectHeroesScene = new Scene(selectHeroesRoot);
		skip.setOnMouseClicked(e -> {
			primaryStage.setScene(selectHeroesScene);
			selectHeroesScene.setCursor(new ImageCursor(cursor));
			primaryStage.setFullScreen(true);
			primaryStage.show();
		});

		selectHeroesRoot.setBackground(bGround2);

		backtohomepage.setOnAction(e -> {
			// counter=0;
			letsbegin.setVisible(false);
			herocliked = false;
			L1.setText("Select the Hero you\nwant to start with!");
			L1.setTextFill(Color.WHITE);
			L2.setText("");
			L2.setTextFill(Color.WHITE);
			L22.setText("");
			L22.setTextFill(Color.WHITE);
			L3.setText("");
			L3.setTextFill(Color.WHITE);
			L4.setText("");
			L4.setTextFill(Color.WHITE);
			L5.setText("");
			L5.setTextFill(Color.WHITE);
			reset();
			primaryStage.setScene(homeScene);
			homeScene.setCursor(new ImageCursor(cursor));
			primaryStage.setFullScreen(true);
			primaryStage.show();
		});

		// Making Game Scene

		BorderPane gameroot = new BorderPane();
		Scene game = new Scene(gameroot);

		// Making Pause Scene

		Button Pause = new Button("Pause");
		Pause.setCursor(new ImageCursor(hoverCursor));
		Pause.setFont(f20);
		Pause.setTextFill(Color.WHITE);
		Pause.setStyle("-fx-background-color: BROWN ;");

		GridPane Pauseroot = new GridPane();
		Pauseroot.setAlignment(Pos.CENTER);

		VBox stack2 = new VBox();
		stack2.setSpacing(100);

		Button ResumeGame = new Button("Resume Game");
		ResumeGame.setCursor(new ImageCursor(hoverCursor));
		ResumeGame.setTextFill(Color.WHITE);
		ResumeGame.setFont(f40);
		ResumeGame.setStyle("-fx-background-color: BROWN ;");
		ResumeGame.setPrefSize(500, 100);

		Label letsgo = new Label();
		letsgo.setTextFill(Color.WHITE);
		letsgo.setFont(f40);

		Button backtohomepage2 = new Button("Back to home page");
		backtohomepage2.setCursor(new ImageCursor(hoverCursor));
		backtohomepage2.setTextFill(Color.WHITE);
		backtohomepage2.setFont(f40);
		backtohomepage2.setStyle("-fx-background-color: BROWN ;");
		backtohomepage2.setPrefSize(500, 100);

		Label Progresswarning = new Label();
		Progresswarning.setTextFill(Color.WHITE);
		Progresswarning.setFont(f30);

		Button help = new Button("Help");
		help.setCursor(new ImageCursor(hoverCursor));
		help.setTextFill(Color.WHITE);
		help.setFont(f40);
		help.setStyle("-fx-background-color: BROWN ;");
		help.setPrefSize(500, 100);

		stack2.getChildren().addAll(ResumeGame, letsgo, backtohomepage2, Progresswarning, help);
		Pauseroot.add(stack2, 0, 0);

		Pauseroot.setBackground(bGround2);

		Scene PauseScene = new Scene(Pauseroot);

		Pause.setOnAction(e -> {
			primaryStage.setScene(PauseScene);
			PauseScene.setCursor(new ImageCursor(cursor));
			primaryStage.setFullScreen(true);
			primaryStage.show();
		});
		ResumeGame.setOnAction(e -> {
			primaryStage.setScene(game);
			game.setCursor(new ImageCursor(cursor));
			primaryStage.setFullScreen(true);
			primaryStage.show();
		});

		ResumeGame.setOnMouseEntered(e -> {
			letsgo.setText("           Let's go!");
		});

		ResumeGame.setOnMouseExited(e -> {
			letsgo.setText("");
		});

		backtohomepage2.setOnAction(e -> {
			// counter=0;
			letsbegin.setVisible(false);
			herocliked = false;
			L1.setText("Select the Hero you\nwant to start with!");
			L1.setTextFill(Color.WHITE);
			L2.setText("");
			L2.setTextFill(Color.WHITE);
			L22.setText("");
			L22.setTextFill(Color.WHITE);
			L3.setText("");
			L3.setTextFill(Color.WHITE);
			L4.setText("");
			L4.setTextFill(Color.WHITE);
			L5.setText("");
			L5.setTextFill(Color.WHITE);
			reset();
			primaryStage.setScene(homeScene);
			homeScene.setCursor(new ImageCursor(cursor));
			primaryStage.setFullScreen(true);
			primaryStage.show();
		});
		backtohomepage2.setOnMouseEntered(e -> {
			Progresswarning.setText("  all progress will be lost!");
		});
		backtohomepage2.setOnMouseExited(e -> {
			Progresswarning.setText("");
		});

		// Making Help Scene

		GridPane helproot = new GridPane();
		helproot.setAlignment(Pos.CENTER);

		VBox stack3 = new VBox();
		stack3.setSpacing(100);

		Button Controls = new Button("Controls");
		Controls.setCursor(new ImageCursor(hoverCursor));
		Controls.setFont(f40);
		Controls.setTextFill(Color.WHITE);
		Controls.setStyle("-fx-background-color: BROWN ;");
		Controls.setPrefSize(500, 100);

		Button howtowin = new Button("How to win");
		howtowin.setCursor(new ImageCursor(hoverCursor));
		howtowin.setFont(f40);
		howtowin.setTextFill(Color.WHITE);
		howtowin.setStyle("-fx-background-color: BROWN ;");
		howtowin.setPrefSize(500, 100);

		Button Okay3 = new Button("Okay");
		Okay3.setCursor(new ImageCursor(hoverCursor));
		Okay3.setFont(f40);
		Okay3.setTextFill(Color.WHITE);
		Okay3.setStyle("-fx-background-color: BROWN ;");
		Okay3.setPrefSize(500, 100);

		stack3.getChildren().addAll(howtowin, Okay3);
		helproot.add(stack3, 0, 0);

		helproot.setBackground(bGround2);

		// Making Controls Scene inside Help Scene

		GridPane helproot1 = new GridPane();
		helproot1.setAlignment(Pos.CENTER);

		VBox stack4 = new VBox();
		stack4.setSpacing(100);

		Label Control1 = new Label("Press A to Attack");
		Control1.setFont(f40);
		Control1.setTextFill(Color.WHITE);
		Label Control2 = new Label("Press C to Cure");
		Control2.setFont(f40);
		Control2.setTextFill(Color.WHITE);
		Label Control3 = new Label("Press S to use Special Action");
		Control3.setFont(f40);
		Control3.setTextFill(Color.WHITE);
		Label Control4 = new Label("Press E to use End Turn");
		Control4.setFont(f40);
		Control4.setTextFill(Color.WHITE);

		Button Okay = new Button("Okay!");
		Okay.setCursor(new ImageCursor(hoverCursor));
		Okay.setFont(f40);
		Okay.setTextFill(Color.WHITE);
		Okay.setStyle("-fx-background-color: BROWN ;");
		Okay.setPrefSize(500, 100);

		stack4.getChildren().addAll(Control1, Control2, Control3, Control4, Okay);
		helproot1.add(stack4, 0, 0);

		helproot1.setBackground(bGround2);

		// Making a howtowin Scene inside HelpScene

		GridPane helproot2 = new GridPane();
		helproot2.setAlignment(Pos.CENTER);

		VBox stack5 = new VBox();
		stack5.setSpacing(50);

		Label message1 = new Label("remember you only win");
		message1.setFont(f30);
		message1.setTextFill(Color.WHITE);
		Label message2 = new Label("if you collect all the Vaccines on the map");
		message2.setFont(f30);
		message2.setTextFill(Color.WHITE);
		Label message3 = new Label("and are in control of atleast 5 heroes!");
		message3.setFont(f30);
		message3.setTextFill(Color.WHITE);

		Button Okay2 = new Button("Okay!");
		Okay2.setCursor(new ImageCursor(hoverCursor));
		Okay2.setFont(f40);
		Okay2.setTextFill(Color.WHITE);
		Okay2.setStyle("-fx-background-color: BROWN ;");
		Okay2.setPrefSize(500, 100);

		stack5.getChildren().addAll(message1, message2, message3, Okay2);
		helproot2.add(stack5, 0, 0);

		helproot2.setBackground(bGround2);

		Scene helpscene1 = new Scene(helproot);

		Scene helpscene2 = new Scene(helproot1);

		Scene helpscene3 = new Scene(helproot2);

		Okay.setOnAction(e -> {
			primaryStage.setScene(helpscene1);
			helpscene1.setCursor(new ImageCursor(cursor));
			primaryStage.setFullScreen(true);
			primaryStage.show();
		});

		Okay2.setOnAction(e -> {
			primaryStage.setScene(helpscene1);
			helpscene1.setCursor(new ImageCursor(cursor));
			primaryStage.setFullScreen(true);
			primaryStage.show();
		});

		Okay3.setOnAction(e -> {
			primaryStage.setScene(PauseScene);
			PauseScene.setCursor(new ImageCursor(cursor));
			primaryStage.setFullScreen(true);
			primaryStage.show();
		});
		help.setOnAction(e -> {
			primaryStage.setScene(helpscene1);
			helpscene1.setCursor(new ImageCursor(cursor));
			primaryStage.setFullScreen(true);
			primaryStage.show();
		});

		Controls.setOnAction(e -> {
			primaryStage.setScene(helpscene2);
			helpscene2.setCursor(new ImageCursor(cursor));
			primaryStage.setFullScreen(true);
			primaryStage.show();
		});

		howtowin.setOnAction(e -> {
			primaryStage.setScene(helpscene3);
			helpscene3.setCursor(new ImageCursor(cursor));
			primaryStage.setFullScreen(true);
			primaryStage.show();
		});

		// Completing Game Scene

		Button EndTurn = new Button("End Turn");
		EndTurn.setCursor(new ImageCursor(hoverCursor));
		EndTurn.setFont(f30);
		EndTurn.setTextFill(Color.WHITE);
		EndTurn.setStyle("-fx-background-color: BROWN;");

		letsbegin.setOnAction(e -> {
			primaryStage.setScene(game);
			game.setCursor(new ImageCursor(cursor));
			try {

				startScene(game);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			primaryStage.setFullScreen(true);
			primaryStage.show();
		});

		HBox top = new HBox();
		top.setSpacing(290);
		message = new Label();
		message.setPrefWidth(52.5 * 15);
		message.setTranslateX(-88);
		message.setAlignment(Pos.CENTER);
		message.setFont(f16);
		message.setTextFill(Color.WHITE);
		// message.setTranslateY()
		// top.getChildren().addAll(Pause, message);

		GridPane detailsroot = new GridPane();
		detailsroot.setAlignment(Pos.CENTER);
		VBox details = new VBox();
		details.setPrefWidth(250);
		details.setSpacing(80);

		name = new Label("Name: ");
		name.setFont(f20);
		name.setTextFill(Color.WHITE);
		type = new Label("Type: ");
		type.setFont(f20);
		type.setTextFill(Color.WHITE);
		actions = new Label("Remaining actions: ");
		actions.setFont(f20);
		actions.setTextFill(Color.WHITE);
		currentHp = new Label("Current Hp: ");
		currentHp.setFont(f20);
		currentHp.setTextFill(Color.WHITE);
		attackDmg = new Label("Attack Damage: ");
		attackDmg.setFont(f20);
		attackDmg.setTextFill(Color.WHITE);
		supplies = new Label("Supplies: ");
		supplies.setFont(f20);
		supplies.setTextFill(Color.WHITE);
		vaccines = new Label("Vaccines: ");
		vaccines.setFont(f20);
		vaccines.setTextFill(Color.WHITE);

		details.getChildren().addAll(name, type, actions, currentHp, attackDmg, supplies, vaccines);
		detailsroot.add(stack4, 0, 0);

		detailsroot.setBackground(bGround2);

		details.setTranslateY(75);
		details.setTranslateX(20);

		rheroes = new MenuButton("My Heroes");
		rheroes.setCursor(new ImageCursor(hoverCursor));
		rheroes.setFont(f30);
		rheroes.setTextFill(Color.WHITE);
		rheroes.setAlignment(Pos.CENTER);
		rheroes.setStyle("-fx-background-color: BROWN;");
		rheroes.setPrefSize(400, 50);

		////// put under yalla in let's begin

		ObservableList<MenuItem> items = rheroes.getItems();
		for (MenuItem item : items)
			rheroes.getItems().remove(item);

		for (int i = 0; i < Game.heroes.size(); i++) {
			Hero h = Game.heroes.get(i);

			String t = new String();

			if (h instanceof Fighter) {
				t = "Type: Fighter";
			}
			if (h instanceof Explorer) {
				t = "Type: Explorer";
			}
			if (h instanceof Medic) {
				t = "Type: Medic";
			}

			MenuItem n1 = new MenuItem("Name: " + h.getName() + " Type: " + t + " attackDmg: "
					+ Integer.toString(h.getAttackDmg()) + " maxActions: " + Integer.toString(h.getMaxActions())
					+ " currentHp: " + Integer.toString(h.getCurrentHp()));
			// n1.setStyle("-fx-background-color: CHOCOLATE;");
			rheroes.getItems().add(n1);
		}

		top.getChildren().addAll(Pause, message, rheroes);

		////////////////////////////////////////////////////

		VBox stack8 = new VBox();

		stack8.setSpacing(50);

		GridPane helproot10 = new GridPane();
		helproot10.setAlignment(Pos.CENTER);

		VBox stack9 = new VBox();
		stack9.setSpacing(100);

		Label Control11 = new Label("Press A to Attack");
		Control11.setFont(f40);
		Control11.setTextFill(Color.WHITE);
		Label Control22 = new Label("Press C to Cure");
		Control22.setFont(f40);
		Control22.setTextFill(Color.WHITE);
		Label Control33 = new Label("Press S to use Special Action");
		Control33.setFont(f40);
		Control33.setTextFill(Color.WHITE);
		Label Control44 = new Label("Press E to use End Turn");
		Control44.setFont(f40);
		Control44.setTextFill(Color.WHITE);

		Button Okay30 = new Button("Okay!");
		Okay30.setCursor(new ImageCursor(hoverCursor));
		Okay30.setFont(f55);
		Okay30.setTextFill(Color.WHITE);
		Okay30.setStyle("-fx-background-color: BROWN ;");
		Okay30.setPrefSize(500, 100);

		Okay30.setOnAction(e -> {
			primaryStage.setScene(game);
			game.setCursor(new ImageCursor(cursor));
			primaryStage.setFullScreen(true);
			primaryStage.show();
		});

		stack9.getChildren().addAll(Control11, Control22, Control33, Control44, Okay30);
		helproot10.add(stack9, 0, 0);

		helproot10.setBackground(bGround2);

		Scene helpscene10 = new Scene(helproot10);

		Button Controls5 = new Button("Controls");
		Controls5.setCursor(new ImageCursor(hoverCursor));
		Controls5.setFont(f40);
		Controls5.setTextFill(Color.WHITE);
		Controls5.setStyle("-fx-background-color: BROWN ;");
		Controls5.setPrefSize(400, 50);

		Controls5.setOnAction(e -> {
			primaryStage.setScene(helpscene10);
			helpscene10.setCursor(new ImageCursor(cursor));
			primaryStage.setFullScreen(true);
			primaryStage.show();
		});


		stack8.getChildren().addAll(Controls5, rheroes);

		Image img3 = new Image("/resources/bgmap.png");
		BackgroundImage bImg3 = new BackgroundImage(img3, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
				BackgroundPosition.CENTER, BackgroundSize.DEFAULT);
		Background bGround3 = new Background(bImg3);

		gameroot.setRight(stack8);
		gameroot.setLeft(details);
		gameroot.setTop(top);
		// gameroot.setBottom(EndTurn);
		gameroot.setBackground(bGround3);

	}

	public static Label name;
	public static Label type;
	public static Label actions;
	public static Label currentHp;
	public static Label attackDmg;
	public static Label supplies;
	public static Label vaccines;
	public static MenuButton rheroes;

	public static void updateDetails() {
		if (playingHero == null) {
			name.setText("Name: ");
			type.setText("Type: ");
			actions.setText("Remaining Actions: ");
			currentHp.setText("Current Hp: ");
			attackDmg.setText("Attack Damage: ");
			supplies.setText("Supplies: ");
			vaccines.setText("Vaccines: ");
		} else {
			name.setText("Name: " + playingHero.getName());
			type.setText("Type: " + playingHero.getClass().getSimpleName());
			actions.setText("Remaining Actions: " + playingHero.getActionsAvailable());
			currentHp.setText("Current Hp: " + playingHero.getCurrentHp());
			attackDmg.setText("Attack Damage: " + playingHero.getAttackDmg());
			supplies.setText("Supplies: " + playingHero.getSupplyInventory().size());
			vaccines.setText("Vaccines: " + playingHero.getVaccineInventory().size());
		}

		int size = rheroes.getItems().size();
		for (int i = 0; i < size; i++)
			rheroes.getItems().remove(0);

		for (int i = 0; i < Game.heroes.size(); i++) {

			Hero h = Game.heroes.get(i);

			String t = new String();

			if (h instanceof Fighter) {
				t = "Type: Fighter";
			}
			if (h instanceof Explorer) {
				t = "Type: Explorer";
			}
			if (h instanceof Medic) {
				t = "Type: Medic";
			}

			MenuItem n1 = new MenuItem("Name: " + h.getName() + " Type: " + t + " attackDmg: "
					+ Integer.toString(h.getAttackDmg()) + " maxActions: " + Integer.toString(h.getMaxActions())
					+ " currentHp: " + Integer.toString(h.getCurrentHp()));
			// n1.setStyle("-fx-background-color: CHOCOLATE;");
			rheroes.getItems().add(n1);
		}
	}

	public static void checkNewZombies() {
		// charactercell character == zombie but imageview image = null
		for (int i = 0; i < 15; i++)
			for (int j = 0; j < 15; j++) {
				MapCell mapCell = tiles.getCell(i, j);
				Cell cell = Game.map[i][j];
				if (cell instanceof CharacterCell && ((CharacterCell) cell).getCharacter() instanceof Zombie
						&& mapCell.getImageAboveTile().getImage() == null)
					mapCell.getImageAboveTile().setImage(zombie);
			}
	}

	public static void reset() {
		playingHero = null;
		waitingForTarget = false;
		Game.heroes = new ArrayList<Hero>();
		Game.availableHeroes = new ArrayList<Hero>();
		Game.zombies = new ArrayList<Zombie>();
		try {
			Game.loadHeroes(Main.class.getResourceAsStream("/resources/Heroes.csv"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void checkDeadCharacters() {
		// charactercell character == null but imageview still exists
		for (int i = 0; i < 15; i++)
			for (int j = 0; j < 15; j++) {
				MapCell mapCell = tiles.getCell(i, j);
				Cell cell = Game.map[i][j];
				if (cell instanceof CharacterCell && ((CharacterCell) cell).getCharacter() == null
						&& mapCell.getImageAboveTile().getImage() != null)
					animateDeath(mapCell);
			}

		if (playingHero != null && playingHero.getCurrentHp() == 0)
			playingHero = null;
	}

	private static int messageCounter = 0;

	private static Dictionary<Label, Integer> errors = new Hashtable<>();

	private static void showMessage(String text) {
		Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(0), event -> {
			message.setText(text);
			message.setVisible(true);
			messageCounter++;
		}), new KeyFrame(Duration.seconds(2), event -> {
			if (messageCounter == 1)
				message.setVisible(false);
			messageCounter--;
		}));
		timeline.play();
	}

	private static void showError(Label label) {
		Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(0), event -> {
			if (errors.get(label) != null)
				errors.put(label, errors.get(label) + 1);
			else
				errors.put(label, 1);
			label.setTextFill(Color.RED);
		}), new KeyFrame(Duration.seconds(2), event -> {
			if (errors.get(label) == 1)
				label.setTextFill(Color.WHITE);
			errors.put(label, errors.get(label) - 1);
		}));
		timeline.play();
	}

	private static void animateDeath(MapCell cell) {
		Timeline skull = new Timeline(
				new KeyFrame(Duration.ZERO,
						event -> cell.getImageAboveTile().setImage(new Image("/resources/brook.png"))),
				new KeyFrame(Duration.seconds(1), event -> cell.getImageAboveTile().setImage(null)));
		skull.play();
	}

	private static void animateFire(int x, int y) {
		ImageView trap = new ImageView("/resources/boom.gif");
		tiles.add(trap, x, y);

		Timeline skull = new Timeline(new KeyFrame(Duration.seconds(1), event -> tiles.getChildren().remove(trap)));
		skull.play();
	}

	public static void medicHeal() {
		try {
			playingHero.useSpecial();
			updateDetails();
			System.out.println("Max HP: " + playingHero.getTarget().getCurrentHp());
			playingHero.setTarget(null);

		} catch (Exception ex) {
			if (ex.getMessage() == null)
				ex.printStackTrace();

			if (ex.getMessage().contains("action point"))
				showError(actions);

			if (ex.getMessage().contains("supply"))
				showError(supplies);

			if (ex.getMessage().contains("vaccine"))
				showError(vaccines);

			showMessage("\n" + ex.getMessage());
			playingHero.setTarget(null);
		}
	}

	public static void zabbat(Button button, boolean isTransparent) {
		if (isTransparent)
			button.setStyle("-fx-background-color: transparent;");
		button.setCursor(new ImageCursor(hoverCursor));
		button.setOnMouseEntered(e -> {
			button.setTextFill(Color.ANTIQUEWHITE);
		});
		button.setOnMouseExited(e -> {
			button.setTextFill(Paint.valueOf("0x333333ff"));
		});
	}

	public static void zabbat(Label label, boolean isTransparent) {
		if (isTransparent)
			label.setStyle("-fx-background-color: transparent;");
		label.setCursor(new ImageCursor(hoverCursor));
		label.setOnMouseEntered(e -> {
			label.setTextFill(Color.ANTIQUEWHITE);
		});
		label.setOnMouseExited(e -> {
			label.setTextFill(Paint.valueOf("0x333333ff"));
		});
	}

	public static Scene createScene(Parent root) {
		Scene scene = new Scene(root);
		scene.setCursor(new ImageCursor(cursor));
		return scene;
	}

	public static void main(String[] args) {
		launch(args);
	}
}