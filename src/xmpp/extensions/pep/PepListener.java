/*
 * PepListener.java
 *
 * Created on 30.04.2008, 21:37
 *
 * Copyright (c) 2005-2008, Eugene Stahov (evgs), http://bombus-im.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * You can also redistribute and/or modify this program under the
 * terms of the Psi License, specified in the accompanied COPYING
 * file, as published by the Psi Project; either dated January 1st,
 * 2005, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package xmpp.extensions.pep;

import Client.*;
import Mood.Moods;
import com.alsutton.jabber.*;
import com.alsutton.jabber.datablocks.*;
import java.util.*;

public class PepListener implements JabberBlockListener{
    
    /** Creates a new instance of PepListener */
    public PepListener() { }

    public int blockArrived(JabberDataBlock data) {
        if (!(data instanceof Message)) return BLOCK_REJECTED;
        //if (!data.getTypeAttribute().equals("headline")) return BLOCK_REJECTED;
        
        JabberDataBlock event=data.findNamespace("event", "http://jabber.org/protocol/pubsub#event");
        if (event==null) return BLOCK_REJECTED;
        
        String from=data.getAttribute("from");

        String id=null;
        
        StringBuffer result=new StringBuffer();

        boolean  tuneVaule=false;
        JabberDataBlock tune=extractEvent(event, "tune", "http://jabber.org/protocol/tune");
        if (tune!=null) {
            result.append((char)0x266a).append(' ');
            
            if (tune.getChildBlocks()==null) result.append("(silence)");
            else {
                String src=tune.getChildBlockText("source");
                
                result.append(tune.getChildBlockText("title"))
                      .append(" - ")
                      .append(tune.getChildBlockText("artist"));
                if (src.length()>0) {
                    result.append(" (").append(src).append(')');
                }
                
                tuneVaule=true;
            }
            
            System.out.println(from+": "+result.toString());
        }

        int moodIndex=-1;
        JabberDataBlock mood=extractEvent(event, "mood", "http://jabber.org/protocol/mood");
        
        String tag=null;
        String moodText = "";
        
        if (mood!=null) {
            result.append(":) ");
            
            try {
                for (Enumeration e=mood.getChildBlocks().elements(); e.hasMoreElements();) {
                    JabberDataBlock child=(JabberDataBlock)e.nextElement();
                    tag=child.getTagName();
                    if (tag.equals("text")) continue;
                    
                    moodIndex=Moods.getInstance().getMoodIngex(tag);
                    
                    id=mood.getParent().getAttribute("id");
                }
            } catch (Exception ex) {
                moodIndex=Moods.getInstance().getMoodIngex("-");
            }
            
            moodText=mood.getChildBlockText("text");
            result.append(Moods.getInstance().getMoodLabel(moodIndex)) 
                  .append(" - ")
                  .append(moodText);
           
            System.out.println(from+": "+result.toString());
        }

        Msg m=new Msg(Msg.MESSAGE_TYPE_HISTORY, from, null, result.toString());
        
        Vector hContacts=StaticData.getInstance().roster.getHContacts();
        synchronized (hContacts) {
            Jid j=new Jid(from);
            for (Enumeration e=hContacts.elements();e.hasMoreElements();){
                Contact c=(Contact)e.nextElement();
                if (c.jid.equals(j, false)) {
                    if (mood!=null) {
                        c.pepMood=moodIndex;
                        
                        if (c.getGroupType()==Groups.TYPE_SELF) {
                            if (id!=null) Moods.getInstance().myMoodId=id;
                            Moods.getInstance().myMoodName=tag;
                            Moods.getInstance().myMoodName=moodText;
                        }
                    }
                    if (tune!=null) c.pepTune=tuneVaule;
                    c.addMessage(m);
                }
            }
        }
        
        StaticData.getInstance().roster.redraw();
        
        return BLOCK_PROCESSED;
    }
 
    JabberDataBlock extractEvent(JabberDataBlock data, String tagName, String xmlns) {
        JabberDataBlock items=data.getChildBlock("items");
        if (items==null) return null;
        if (!xmlns.equals(items.getAttribute("node"))) return null;
        JabberDataBlock item=items.getChildBlock("item");
        if (item==null) return new JabberDataBlock();
        return item.findNamespace(tagName, xmlns);
    }
}
