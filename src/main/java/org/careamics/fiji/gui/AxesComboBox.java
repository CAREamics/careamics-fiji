package org.careamics.fiji.gui;

import javax.swing.JComboBox;



public class AxesComboBox extends JComboBox<String>{
    private static final String[] AXES_LIST = {
        "C", "Z", "T",
        "CZ", "CT", "ZC", "ZT", "TC", "TZ",
        "CZT", "CTZ", "ZCT", "ZTC", "TCZ", "TZC"
    };
    private static final String XY = "XY";

    public AxesComboBox() {
        this.addItem(XY);
        for (String axis : AXES_LIST) {
            this.addItem(XY + axis);
        }
        
        setEditable(false);
    }

}
