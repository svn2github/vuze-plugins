package com.azureus.plugins.aznetmon.util;

import java.io.*;

/**
 * Created on Aug 28, 2007
 * Created by Alan Snyder
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 * <p/>
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 * <p/>
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 */

/**
 * Wrapper class to deal with corner-cases of Runtime.exec() call.
 * Still cannot pipe serveral things together, but it will give a
 * result for each call.
 */
public class RuntimeExec
{

    final String[] command;
    File currentWorkingDir=null;

    StringBuffer sbStdOut = new StringBuffer();
    StringBuffer sbStdErr = new StringBuffer();



    public RuntimeExec(String[] _cmd){

        command = _cmd;

    }

    public RuntimeExec(String[] _cmd, File workingDir){
        this(_cmd);
        currentWorkingDir = workingDir;
    }

    public void setTimeout(int inMillSec){
        //ToDo: need to implement this.
    }


    public int execDontBlock(){
        int exit = -1;

        try{
            Process p;
            if( currentWorkingDir == null){
                p = Runtime.getRuntime().exec(command);
            }else{
                p = Runtime.getRuntime().exec(command,null,currentWorkingDir);
            }

            exit = p.waitFor();

        }catch(IOException ioe){
            System.out.println(" Had : "+ioe+" while running: "+ command);
            ioe.printStackTrace();
        }catch(InterruptedException ie){
            //just exit.
        }catch(Throwable t){
            System.out.println(" Had : "+t+" cmd="+ command);
            t.printStackTrace();
        }finally{

        }

        return exit;
    }

    /**
     * NOTE: A blocking call with will wait for the result.
     * @return - exitVal of process call.  Usually 0 is good.
     */
    public int exec(){

        int exit = -1;


        StreamConsumer scStdOut=null;
        StreamConsumer scStdErr=null;
        try{
            Process p;
            if( currentWorkingDir == null){
                p = Runtime.getRuntime().exec(command);
            }else{
                p = Runtime.getRuntime().exec(command,null,currentWorkingDir);
            }

            scStdOut = new StreamConsumer( p.getInputStream(), sbStdOut );
            scStdErr = new StreamConsumer( p.getErrorStream(), sbStdErr );

            scStdOut.start();
            scStdErr.start();

            exit = p.waitFor();

            if( !scStdOut.isDone() ){

                try{Thread.sleep(50);}
                catch(InterruptedException ie){}

                //For debug.
                if( !scStdOut.isDone() ){
                    System.out.println("didn't finish reading buffer.");
                }

            }

        }catch(IOException ioe){
            System.out.println(" Had : "+ioe+" while running: "+ command);
            ioe.printStackTrace();
        }catch(InterruptedException ie){
            //just exit.
        }catch(Throwable t){
            System.out.println(" Had : "+t+" cmd="+ command);
            t.printStackTrace();
        }finally{

            //Should we make SURE the processes have finished here!!!
            if( scStdOut!=null ){
                scStdOut.setDone();
            }

            if( scStdErr!=null ){
                scStdErr.setDone();
            }

        }

        return exit;

    }//exec

    /**
     * Call after exec completes. To get the buffer.
     * @return -
     */
    public String getStdOut(){
        return sbStdOut.toString();
    }

    /**
     * Call after exec completes to get the buffer.
     * @return -
     */
    public String getStdErr(){
        return sbStdErr.toString();
    }



    static class StreamConsumer extends Thread{

        final StringBuffer sb;
        final InputStream in;

        boolean isDone = false;

        public StreamConsumer(InputStream _in,StringBuffer _sb){
            in = _in;
            sb = _sb;
        }

        public void run(){

            try{

                String enc = System.getProperty("file.encoding");
                InputStreamReader isr = new InputStreamReader(in,enc);

                BufferedReader br = new BufferedReader(isr);
                String line;
                while( (line=br.readLine())!=null && !isDone ){
                    sb.append(line).append("\n");
                }

                isDone = true;

            }catch(Throwable t){
                sb.append("######## ERROR #######");
                sb.append(t.getMessage());
                t.printStackTrace();
            }finally{

                if(in!=null){
                    try{
                        in.close();
                    }catch(IOException ioe){}
                }

            }

        }//run

        /**
         * Get status of reading buffer.
         * @return -
         */
        public boolean isDone(){
            return isDone;
        }

        /**
         * Call when closing this stream.
         */
        public void setDone(){
            isDone = true;
        }

    }//static class


}
