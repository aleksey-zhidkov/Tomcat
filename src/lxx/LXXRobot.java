/*
 * Copyright (c) 2011 Alexey Zhidkov (Jdev). All Rights Reserved.
 */

package lxx;

import lxx.utils.APoint;
import lxx.utils.LXXPoint;

/**
 * User: jdev
 * Date: 07.11.2009
 */
public interface LXXRobot extends APoint {

    long getTime();

    String getName();

    boolean isAlive();

    boolean equals(Object another);

    int hashCode();

    LXXPoint getPosition();

    double getFirePower();

    int getRound();

}
