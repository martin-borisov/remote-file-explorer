package mb.client.webdav.components;

import org.kordamp.ikonli.javafx.FontIcon;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class GridView extends TilePane {
    
    private VBox selectedItem;

    public GridView(double hgap, double vgap) {
        super(hgap, vgap);
    }

    public Node addItem(String label, FontIcon icon, int width, Object obj) {
        
        // Default icon
        icon.setIconSize(width);
        
        // Label
        Label lbl = new Label(label);
        lbl.setMaxWidth(width);
        
        VBox box = new VBox(icon, lbl);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10));
        box.setUserData(obj);
        getChildren().add(box);
        
        // Grid selection
        box.setOnMousePressed(event -> {
            deselectPrevious();
            selectedItem = box;
            markSelected(box);
        });
        
        return box;
    }

    public VBox getSelectedItem() {
        return selectedItem;
    }
    
    private void markSelected(VBox box) {
        box.setBackground(new Background(new BackgroundFill(Color.DARKGREY, new CornerRadii(5), Insets.EMPTY)));
    }
    
    private void deselectPrevious() {
        if(selectedItem != null) {
            selectedItem.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY)));
        }
    }
    
    

}
