/*
 * MIDPTextBox.java
 *
 * Created on 26 ���� 2005 �., 20:56
 */

package ui;

import javax.microedition.lcdui.*;

/**
 *
 * @author  Eugene Stahov
 * @version
 */
public class MIDPTextBox implements CommandListener {
    
    private Display display;
    private Displayable parentView;
    
    protected Command cmdCancel=new Command("Cancel", Command.BACK, 99);
    protected Command cmdOK=new Command("OK", Command.OK, 1);
    
    private TextBox t;
    
    private TextBoxNotify tbn;

    /**
     * constructor
     */
    public interface TextBoxNotify {
        void OkNotify(String text_return);
    }
   
    public MIDPTextBox(Display display, String title, String text, TextBoxNotify tbn ) {
        t=new TextBox(title, text, 50, TextField.ANY);
        
        this.display=display;
        this.tbn=tbn;
        
        t.addCommand(cmdOK);
        t.addCommand(cmdCancel);
        
        t.setCommandListener(this);
            

        parentView=display.getCurrent();
        display.setCurrent(t);
    }
    
    /**
     * Called when action should be handled
     */
    public void commandAction(Command command, Displayable displayable) {
        if (command==cmdCancel) { destroyView(); return;}
        if (command==cmdOK) { tbn.OkNotify(t.getString()); destroyView(); return;}
    }

    public void destroyView(){
        if (display!=null)   display.setCurrent(parentView);
    }
}
