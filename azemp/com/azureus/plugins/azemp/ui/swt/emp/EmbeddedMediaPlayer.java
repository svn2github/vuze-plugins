/**
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 *
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
 *
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */

package com.azureus.plugins.azemp.ui.swt.emp;

import org.eclipse.swt.widgets.Composite;

/**
 * @author TuxPaper
 * @created Aug 9, 2007
 *
 */
public interface EmbeddedMediaPlayer
{
	public final int ACTION_FLIP_PAUSE = 0;
	public final int ACTION_STOP = 1;
	public final int ACTION_FLIP_FULLSCREEN = 2;
	public final int ACTION_FLIP_MUTE = 3;
	public final int ACTION_PAUSE = 4;
	public final int ACTION_PLAY = 5;
	
	public void init(Composite control, boolean useSWTFullScreen) throws Throwable;
	
	public void runAction(int action);

	/**
	 * @param s
	 *
	 * @since 3.0.1.7
	 */
	void sendCommand(String command);

	/**
	 * @param seconds
	 * @param pause
	 *
	 * @since 3.0.1.7
	 */
	void seekAbsolute(float seconds);

	/**
	 * 
	 *
	 * @since 3.0.1.7
	 */
	void delete();

	/**
	 * @return
	 *
	 * @since 3.0.1.7
	 */
	float getLength();

	/**
	 * @return
	 *
	 * @since 3.0.1.7
	 */
	float getPositionSecs();

	/**
	 * @return
	 *
	 * @since 3.0.1.7
	 */
	boolean isFullScreen();

	/**
	 * @param percent
	 * @param pause
	 *
	 * @since 3.0.1.7
	 */
	void seekPercent(float percent);

	/**
	 * @return
	 *
	 * @since 3.0.1.7
	 */
	float getPositionPercent();

	/**
	 * @param mediaLocation
	 * @return
	 *
	 * @since 3.0.1.7
	 */
	boolean loadMedia(String mediaLocation);

	/**
	 * @return
	 *
	 * @since 3.0.1.7
	 */
	boolean isPaused();

	/**
	 * @param val
	 *
	 * @since 3.0.2.1
	 */
	void setVolume(int val);

	/**
	 * @return
	 *
	 * @since 3.0.2.1
	 */
	int getVolume();

	/**
	 * @return
	 *
	 * @since 3.0.2.3
	 */
	boolean isBuffering();

	/**
	 * @return
	 *
	 * @since 3.0.2.3
	 */
	long getStreamPos();
	
	void addListener(EmpListenerFileChanged l);

	void removeListener(EmpListenerFileChanged l);

	/**
	 * @return
	 *
	 * @since 3.0.2.3
	 */
	String getFileName();

	/**
	 * 
	 *
	 * @since 3.0.2.3
	 */
	public boolean isMuted();

	/**
	 * @param booleanParameter
	 *
	 * @since 3.0.2.3
	 */
	public void setMuted(boolean booleanParameter);

	/**
	 * @return
	 *
	 * @since 3.0.2.3
	 */
	public boolean isStopped();

	/**
	 * @return
	 *
	 * @since 3.0.3.5
	 */
	float getFrameDropPct();

	/**
	 * @param l
	 *
	 * @since 3.0.3.5
	 */
	void removeListener(EmpListenerAudio l);

	/**
	 * @param l
	 *
	 * @since 3.0.3.5
	 */
	void addListener(EmpListenerAudio l);

	/**
	 * @param l
	 *
	 * @since 3.0.3.5
	 */
	void addListener(EmpListenerTimeout l);

	/**
	 * @param l
	 *
	 * @since 3.0.3.5
	 */
	void removeListener(EmpListenerTimeout l);


	void addListener(EmpListenerFullScreen l);
	
	void removeListener(EmpListenerFullScreen l);

	/**
	 * @param b
	 *
	 * @since 3.0.4.3
	 */
	public void setAllowSeekAhead(boolean b);

	public void semiClose();

	void addListener(EmpListenerSemiClose l);

	void removeListener(EmpListenerSemiClose l);

	void setExtraDropFrames(long x);

	/**
	 * @param f
	 *
	 * @since 3.0.4.3
	 */
	public void seekRelative(float f);

	/**
	 * @param rel
	 *
	 * @since 3.0.4.3
	 */
	void setVolumeRelative(int rel);
	
	public int[] getVideoResolution();

	public String getVO();
}
