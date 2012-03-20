/*
 * Copyright (c) 2011 Alexey Zhidkov (Jdev). All Rights Reserved.
 */

package lxx.ts_log.attributes.attribute_extractors;

import lxx.EnemySnapshotImpl;
import lxx.LXXRobot;
import lxx.MySnapshotImpl;
import lxx.bullets.LXXBullet;
import lxx.office.Office;

import java.util.List;

/**
 * User: jdev
 * Date: 23.02.2010
 */
public class DistanceBetween implements AttributeValueExtractor {

    public double getAttributeValue(EnemySnapshotImpl enemy, MySnapshotImpl me) {
        return me.aDistance(enemy);
    }

}
