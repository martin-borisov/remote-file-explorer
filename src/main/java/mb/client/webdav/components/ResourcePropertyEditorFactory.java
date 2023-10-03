package mb.client.webdav.components;

import org.apache.commons.io.FileUtils;
import org.controlsfx.control.PropertySheet.Item;
import org.controlsfx.property.editor.AbstractPropertyEditor;
import org.controlsfx.property.editor.DefaultPropertyEditorFactory;
import org.controlsfx.property.editor.PropertyEditor;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;

/**
 * Provides custom editors for better presentation of resource properties
 */
public class ResourcePropertyEditorFactory extends DefaultPropertyEditorFactory {

    @Override
    public PropertyEditor<?> call(Item item) {
        PropertyEditor<?> editor;
        if("size".equals(item.getName())) {
            editor = sizePropertyEditor(item);
        } else if(item.getType() == Boolean.class || item.getType() == java.lang.Boolean.TYPE) {
            editor = super.call(item);
        } else {
            editor = anyPropertyeditor(item);
        }
        return editor;
    }
    
    private AbstractPropertyEditor<Long, Label> sizePropertyEditor(Item item) {
        return new AbstractPropertyEditor<Long, Label>(item, new Label()) {
            public void setValue(Long value) {
                getEditor().setText(FileUtils.byteCountToDisplaySize(value));
            }
            protected ObservableValue<Long> getObservableValue() {
                return new SimpleObjectProperty<Long>();
            }
        };
    }
    
    private AbstractPropertyEditor<Object, Label> anyPropertyeditor(Item item) {
        return new AbstractPropertyEditor<Object, Label>(item, new Label()) {
            public void setValue(Object value) {
                if(value != null) {
                    getEditor().setText(value.toString());
                }
            }
            protected ObservableValue<Object> getObservableValue() {
                return new SimpleObjectProperty<Object>();
            }
        };
    }
}
