/*
 * DnsSrvResolver.java
 *
 * Created on 24.04.2008, 21:55
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

package io;

import Client.StaticData;
import com.ssttr.crypto.MD5;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Hashtable;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import ui.Time;
import util.strconv;

/**
 *
 * @author root
 */
public class DnsSrvResolver {
    
    private final static String resolverUrl="http://bombus-im.org/srv/";
    
    private String server;
    private String resolvedHost;
    private int resolvedPort;
    private long ttl;
    /** Creates a new instance of DnsSrvResolver */
    
    public DnsSrvResolver() {
        resolvedPort=5222;
    }
    
    private String getSrvRecordName() {
        String srv="srv#"+server;
        if (srv.length()>32) return srv.substring(0, 31);
        return srv;
    }
    
    private boolean getCachedSrv() {
        DataInputStream inputStream=NvStorage.ReadFileRecord(getSrvRecordName(), 0);
        try {
            resolvedHost=inputStream.readUTF();
            resolvedPort=inputStream.readInt();
            ttl=inputStream.readLong();
            inputStream.close();
            
            if (ttl>Time.utcTimeMillis()) {
                System.out.println("Srv cache hit");
                return true;
            }
            System.out.println("Srv cache expired");
            
        } catch (Exception e) { 
            System.out.println("Srv cache missed");
        } 
        return false;
    }
    
    private void writeSrvCache() {

        DataOutputStream outputStream=NvStorage.CreateDataOutputStream();
        try {
            outputStream.writeUTF(resolvedHost);
            outputStream.writeInt(resolvedPort);
            outputStream.writeLong(ttl);
        } catch (Exception e) { e.printStackTrace(); }
        NvStorage.writeFileRecord(outputStream, getSrvRecordName(), 0, true);

    }
    
    private boolean askInetSrv() {
        StringBuffer url=new StringBuffer(resolverUrl);
        url.append("?host=").append(server);
        url.append("&client=").append(strconv.urlPrep(Info.Version.getNameVersion()));
        
        url.append("%2F");
        if (Client.Config.getInstance().enableVersionOs) {
            url.append(strconv.urlPrep(Info.Version.getOs()));
        }
        url.append("%2F");
        
        MD5 md5sum=new MD5();
        md5sum.init();
        md5sum.update(StaticData.getInstance().account.getBareJid());
        md5sum.finish();
        
        url.append(md5sum.getDigestHex());
        
    
        System.out.println(url.toString());
        
        try {
            HttpConnection c = (HttpConnection) Connector.open(url.toString());
            
            if (c.getResponseCode()!=HttpConnection.HTTP_OK) return false;
            
            Hashtable ht=new util.StringLoader().hashtableLoader(c.openInputStream());
            
            c.close();
            
            resolvedHost=(String)ht.get("host");
            resolvedPort=Integer.parseInt((String)ht.get("port"));
            ttl=Integer.parseInt((String)ht.get("ttl"))*1000 
                    + Time.utcTimeMillis();

            return true;
        } catch (Exception e) { 
            e.printStackTrace();
        }

        return false;
    }

    public boolean getSrv(String server){
        this.server=server;
        
        if (getCachedSrv()) return true;

        if (!askInetSrv()) return false;
        
        writeSrvCache();

        return true;
    }

    public String getHost() { return resolvedHost; }

    public int getPort() { return resolvedPort; }

   
}
