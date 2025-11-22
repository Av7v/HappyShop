package ci553.happyshop.client.customer;

import ci553.happyshop.catalogue.Product;
import ci553.happyshop.utility.StorageLocation;
import ci553.happyshop.utility.UIStyle;
import ci553.happyshop.utility.WinPosManager;
import ci553.happyshop.utility.WindowBounds;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * The CustomerView is separated into two sections by a line :
 * <p>
 * 1. Search Page – Always visible, allowing customers to browse and search for products.
 * 2. the second page – display either the Trolley Page or the Receipt Page
 * depending on the current context. Only one of these is shown at a time.
 */

public class CustomerView {
    public CustomerController cusController;

    private final int WIDTH = UIStyle.customerWinWidth;
    private final int HEIGHT = UIStyle.customerWinHeight;
    private final int COLUMN_WIDTH = WIDTH / 2 - 10;

    private HBox hbRoot; // Top-level layout manager
    private VBox vbTrolleyPage;  //vbTrolleyPage and vbReceiptPage will swap with each other when need
    private VBox vbReceiptPage;

    TextField tfId; //for user input on the search page. Made accessible so it can be accessed or modified by CustomerModel
    TextField tfName; //for user input on the search page. Made accessible so it can be accessed by CustomerModel

    //four controllers needs updating when program going on
    private ImageView ivProduct; //image area in searchPage
    private Label lbProductInfo;//product text info in searchPage
    private TextArea taTrolley; //in trolley Page
    private TextArea taReceipt;//in receipt page

    // Holds a reference to this CustomerView window for future access and management
    // (e.g., positioning the removeProductNotifier when needed).
    private Stage viewWindow;
    private Media sound;
    private MediaPlayer mediaPlayer;
    private String style =UIStyle.rootStyleColorful;
    private ObservableList<Product> obeProductList; //observable product list
    ListView<Product> obrLvProducts;
    Label laPlaceHolder;

    public void start(Stage window) {
        sound = new Media(new File("src/main/resources/select-button-ui-395763.mp3").toURI().toString());
        mediaPlayer = new MediaPlayer(sound);
        VBox vbSearchPage = createSearchPage();
        vbTrolleyPage = CreateTrolleyPage();
        vbReceiptPage = createReceiptPage();

        // Create a divider line
        Line line = new Line(0, 0, 0, HEIGHT);
        line.setStrokeWidth(4);
        line.setStroke(Color.PINK);
        VBox lineContainer = new VBox(line);
        lineContainer.setPrefWidth(4); // Give it some space
        lineContainer.setAlignment(Pos.CENTER);

        hbRoot = new HBox(10, vbSearchPage, lineContainer, vbTrolleyPage); //initialize to show trolleyPage
        hbRoot.setAlignment(Pos.CENTER);
        hbRoot.setStyle(UIStyle.rootStyleColorful);

        Scene scene = new Scene(hbRoot, WIDTH, HEIGHT);
        window.setScene(scene);
        window.setTitle("🛒 HappyShop Customer Client");
        WinPosManager.registerWindow(window, WIDTH, HEIGHT); //calculate position x and y for this window
        window.show();
        viewWindow = window;// Sets viewWindow to this window for future reference and management.
    }

    private VBox createSearchPage() {
        Label laPageTitle = new Label("Search by Product ID/Name");
        laPageTitle.setStyle(UIStyle.labelTitleStyle);

        tfName = new TextField();
        tfName.setPromptText("eg.:0001 or tv");
        tfName.setStyle(UIStyle.textFiledStyle);
        tfName.setOnAction(actionEvent -> {
            try {
                cusController.doAction("🔍");  //pressing enter can also do search
            } catch (SQLException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        Button btnSearch = new Button("🔍");
        btnSearch.setStyle(UIStyle.buttonStyle);
        btnSearch.setOnAction(this::buttonClicked);
        HBox hbName = new HBox(10,tfName,btnSearch);

        laPlaceHolder = new Label("Search Summary");
        laPlaceHolder.setStyle(UIStyle.labelStyle); //create left-side spacing so that this HBox aligns with others in the layout.
        Button btnAddToTrolley = new Button("Add to trolley");
        btnAddToTrolley.setStyle(UIStyle.buttonStyle);
        btnAddToTrolley.setOnAction(this::buttonClicked);
        HBox hbBtns = new HBox(20, laPlaceHolder, btnAddToTrolley);

        ivProduct = new ImageView("imageHolder.jpg");
        ivProduct.setFitHeight(60);
        ivProduct.setFitWidth(60);
        ivProduct.setPreserveRatio(true); // Image keeps its original shape and fits inside 60×60
        ivProduct.setSmooth(true); //make it smooth and nice-looking

        lbProductInfo = new Label("Thank you for shopping with us.");
        lbProductInfo.setWrapText(true);
        lbProductInfo.setMinHeight(Label.USE_PREF_SIZE);  // Allow auto-resize
        lbProductInfo.setStyle(UIStyle.labelMulLineStyle);
        obeProductList = FXCollections.observableArrayList();
        obrLvProducts = new ListView<>(obeProductList);//ListView proListView observes proList
        obrLvProducts.setPrefHeight(HEIGHT - 100);
        obrLvProducts.setFixedCellSize(50);
        obrLvProducts.setStyle(UIStyle.listViewStyle);
        /**
         * When is setCellFactory() Needed?
         * If you want to customize each row’s content (e.g.,images, buttons, labels, etc.).
         * If you need special formatting (like colors or borders).
         *
         * When is setCellFactory() NOT Needed?
         * Each row is just plain text without images or formatting.
         */
        obrLvProducts.setCellFactory(param -> new ListCell<Product>() {
            @Override
            protected void updateItem(Product product, boolean empty) {
                super.updateItem(product, empty);

                if (empty || product == null) {
                    setGraphic(null);
                    System.out.println("setCellFactory - empty item");
                } else {
                    String imageName = product.getProductImageName(); // Get image name (e.g. "0001.jpg")
                    String relativeImageUrl = StorageLocation.imageFolder + imageName;
                    // Get the full absolute path to the image
                    Path imageFullPath = Paths.get(relativeImageUrl).toAbsolutePath();
                    String imageFullUri = imageFullPath.toUri().toString();// Build the full image Uri

                    ImageView ivPro;
                    try {
                        ivPro = new ImageView(new Image(imageFullUri, 50, 45, true, true)); // Attempt to load the product image
                    } catch (Exception e) {
                        // If loading fails, use a default image directly from the resources folder
                        ivPro = new ImageView(new Image("imageHolder.jpg", 50, 45, true, true)); // Directly load from resources
                    }

                    Label laProToString = new Label(product.toString()); // Create a label for product details
                    HBox hbox = new HBox(10, ivPro, laProToString); // Put ImageView and label in a horizontal layout
                    setGraphic(hbox);  // Set the whole row content
                }
            }
        });

        VBox vbSearchPage = new VBox(15, laPageTitle, hbName, hbBtns, obrLvProducts);
        vbSearchPage.setPrefWidth(COLUMN_WIDTH);
        vbSearchPage.setAlignment(Pos.TOP_CENTER);
        vbSearchPage.setStyle("-fx-padding: 15px;");

        return vbSearchPage;
    }

    private VBox CreateTrolleyPage() {
        Button btnColor = new Button("Change Theme");
        btnColor.setStyle(UIStyle.grayFillBtnStyle);
        btnColor.setPrefWidth(150);
        btnColor.setOnAction(e -> {
            if (style.equals(UIStyle.rootStyleColorful)) {
                style = UIStyle.rootStyleDark;
            } else if (style.equals(UIStyle.rootStyleDark)) {
                style = UIStyle.rootStyleLight;
            } else {
                style = UIStyle.rootStyleColorful;
            }
            viewWindow.getScene().getRoot().setStyle(style);
        });
        Label laPageTitle = new Label("🛒🛒  Trolley 🛒🛒");
        laPageTitle.setStyle(UIStyle.labelTitleStyle);

        taTrolley = new TextArea();
        taTrolley.setEditable(false);
        taTrolley.setPrefSize(WIDTH / 2, HEIGHT - 50);

        Button btnCancel = new Button("Cancel");
        btnCancel.setOnAction(this::buttonClicked);
        btnCancel.setStyle(UIStyle.buttonStyle);

        Button btnCheckout = new Button("Check Out");
        btnCheckout.setOnAction(this::buttonClicked);
        btnCheckout.setStyle(UIStyle.buttonStyle);

        HBox hbBtns = new HBox(10, btnCancel, btnCheckout);
        hbBtns.setStyle("-fx-padding: 15px;");
        hbBtns.setAlignment(Pos.CENTER);

        vbTrolleyPage = new VBox(15,btnColor, laPageTitle, taTrolley, hbBtns);
        vbTrolleyPage.setPrefWidth(COLUMN_WIDTH);
        vbTrolleyPage.setAlignment(Pos.TOP_CENTER);
        vbTrolleyPage.setStyle("-fx-padding: 15px;");
        return vbTrolleyPage;
    }

    private VBox createReceiptPage() {
        Label laPageTitle = new Label("Receipt");
        laPageTitle.setStyle(UIStyle.labelTitleStyle);

        taReceipt = new TextArea();
        taReceipt.setEditable(false);
        taReceipt.setPrefSize(WIDTH / 2, HEIGHT - 50);

        Button btnCloseReceipt = new Button("OK & Close"); //btn for closing receipt and showing trolley page
        btnCloseReceipt.setStyle(UIStyle.buttonStyle);

        btnCloseReceipt.setOnAction(this::buttonClicked);

        vbReceiptPage = new VBox(15, laPageTitle, taReceipt, btnCloseReceipt);
        vbReceiptPage.setPrefWidth(COLUMN_WIDTH);
        vbReceiptPage.setAlignment(Pos.TOP_CENTER);
        vbReceiptPage.setStyle(UIStyle.rootStyleYellow);
        return vbReceiptPage;
    }


    private void buttonClicked(ActionEvent event) {
        try {
            mediaPlayer.stop();
            mediaPlayer.play();
            Button btn = (Button) event.getSource();
            String action = btn.getText();
            if (action.equals("Add to trolley" )&& obrLvProducts.getSelectionModel().getSelectedItem() != null) {
                showTrolleyOrReceiptPage(vbTrolleyPage); //ensure trolleyPage shows if the last customer did not close their receiptPage
            }
            if (action.equals("OK & Close")) {
                showTrolleyOrReceiptPage(vbTrolleyPage);
            }
            cusController.doAction(action);

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    void updateObservableProductList(ArrayList<Product> productList) {
        int proCounter = productList.size();
        System.out.println(proCounter);
        laPlaceHolder.setText(proCounter + " products found");
        laPlaceHolder.setVisible(true);
        obeProductList.clear();
        obeProductList.addAll(productList);
    }
    public void update(String imageName, String searchResult, String trolley, String receipt) {

        ivProduct.setImage(new Image(imageName));
        lbProductInfo.setText(searchResult);
        taTrolley.setText(trolley);
        if (!receipt.equals("")) {
            showTrolleyOrReceiptPage(vbReceiptPage);
            taReceipt.setText(receipt);
        }
    }

    // Replaces the last child of hbRoot with the specified page.
    // the last child is either vbTrolleyPage or vbReceiptPage.
    private void showTrolleyOrReceiptPage(Node pageToShow) {
        int lastIndex = hbRoot.getChildren().size() - 1;
        if (lastIndex >= 0) {
            hbRoot.getChildren().set(lastIndex, pageToShow);
        }
    }

    WindowBounds getWindowBounds() {
        return new WindowBounds(viewWindow.getX(), viewWindow.getY(),
                viewWindow.getWidth(), viewWindow.getHeight());
    }
}
