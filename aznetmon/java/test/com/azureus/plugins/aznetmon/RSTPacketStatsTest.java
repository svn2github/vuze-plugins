package com.azureus.plugins.aznetmon;

import junit.framework.TestCase;
import junit.framework.Assert;
import com.azureus.plugins.aznetmon.main.RSTPacketStats;

/**
 * Created on Dec 21, 2007
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

public class RSTPacketStatsTest extends TestCase
{

    public void testIsTimeToStoreResults()
    {

        RSTPacketStats stats = RSTPacketStats.getInstance();


        //1198277863078L = 14:57
        RSTPacketStats.RSTPacketData data1 = new RSTPacketStats.RSTPacketData();
        data1.timestamp = 1198277863078L;
        data1.deltaConnReset = 3;

        stats.add(data1);

        boolean shouldBeTrue = stats.isTimeToStoreResults();
        Assert.assertTrue("Should be true",shouldBeTrue);

        //1198278263078L = 15:04
        RSTPacketStats.RSTPacketData data2 = new RSTPacketStats.RSTPacketData();
        data2.timestamp = 1198278263078L;
        data2.deltaConnReset = 5;

        stats.add(data2);

        boolean shouldBeFalse = stats.isTimeToStoreResults();
        Assert.assertTrue("Should be false",!shouldBeFalse);

        RSTPacketStats.RSTPacketData res = stats.gatherUnstoredResults();

        Assert.assertTrue("Should be 8",res.nConnReset==8);

    }//

}//RSTPacketStatsTest
