/*
 * Copyright (c) 2011 Alexey Zhidkov (Jdev). All Rights Reserved.
 */

package lxx.office;

import lxx.RobotListener;
import lxx.Tomcat;
import lxx.bullets.BulletManagerListener;
import lxx.bullets.LXXBullet;
import lxx.events.TickEvent;
import lxx.utils.HitRate;
import lxx.utils.LXXUtils;
import lxx.utils.wave.Wave;
import lxx.utils.wave.WaveCallback;
import robocode.*;

/**
 * User: jdev
 * Date: 16.06.2010
 */
public class StatisticsManager implements RobotListener, BulletManagerListener, WaveCallback {

    private static final int[] placeDeathCount = new int[11];
    private static final double[] placeEnergyCount = new double[11];
    private static final int[] placePassed = new int[11];

    private static HitRate myHitRate;
    private static HitRate enemyHitRate;

    private static HitRate myRawHitRate;
    private static HitRate enemyRawHitRate;

    private static HitRate myEnergyHitRate;
    private static HitRate enemyEnergyHitRate;

    private static int wallHits;
    private static int skippedTurns;

    public static int enemyHitsDataCount;
    public static int enemyWavesDataCount;

    private final Office office;
    private final Tomcat tomcat;

    public StatisticsManager(Office office, Tomcat tomcat) {
        this.office = office;
        this.tomcat = tomcat;
        if (myHitRate == null) {
            myHitRate = new HitRate();
            enemyHitRate = new HitRate();

            myEnergyHitRate = new HitRate();
            enemyEnergyHitRate = new HitRate();

            myRawHitRate = new HitRate();
            enemyRawHitRate = new HitRate();
        }

        office.getBulletManager().addListener(this);
        office.getEnemyBulletManager().addListener(this);

    }

    public void onTick() {
        PropertiesManager.setDebugProperty(tomcat.getName() + " static hit rate", String.valueOf(myHitRate));
        PropertiesManager.setDebugProperty(tomcat.getName() + " static energy hit rate", String.valueOf(myEnergyHitRate));
        PropertiesManager.setDebugProperty(tomcat.getName() + " miss count", String.valueOf(myHitRate.getMissCount()));

        if (tomcat.isDuel() && office.getTargetManager().hasDuelOpponent()) {
            PropertiesManager.setDebugProperty(office.getTargetManager().getDuelOpponentName() + " static hit rate", String.valueOf(enemyHitRate));
            PropertiesManager.setDebugProperty(office.getTargetManager().getDuelOpponentName() + " static energy hit rate", String.valueOf(enemyEnergyHitRate));
        }
    }

    public void onRobotDeath() {
        placeEnergyCount[tomcat.getOthers() + 2] = (placeEnergyCount[tomcat.getOthers() + 2] * placePassed[tomcat.getOthers() + 2] + tomcat.getEnergy()) / (placePassed[tomcat.getOthers() + 2] + 1);
        placePassed[tomcat.getOthers() + 2]++;
    }

    public void onDeath() {
        placeDeathCount[tomcat.getOthers() + 1]++;
    }

    public void onWin() {
        placeDeathCount[1]++;
        placePassed[1]++;
    }

    public void onEvent(Event event) {
        if (tomcat.getOthers() > 10) {
            return;
        }
        if (event instanceof WinEvent) {
            onWin();
        } else if (event instanceof DeathEvent) {
            onDeath();
        } else if (event instanceof RobotDeathEvent) {
            onRobotDeath();
        } else if (event instanceof TickEvent) {
            onTick();
        } else if (event instanceof HitWallEvent) {
            wallHits++;
            PropertiesManager.setDebugProperty("Wall hits", String.valueOf(wallHits));
        } else if (event instanceof SkippedTurnEvent) {
            skippedTurns++;
            PropertiesManager.setDebugProperty("Skipped turns", String.valueOf(skippedTurns));
        }
    }

    public void bulletFired(LXXBullet bullet) {
        if (bullet.getSourceState().getName().equals(tomcat.getName())) {
            office.getWaveManager().addCallback(this, bullet.getWave());
        }
    }

    public void bulletHit(LXXBullet bullet) {
        if (bullet.getSourceState().getName().equals(tomcat.getName())) {
            myHitRate.hit();
            myRawHitRate.hit();
            myEnergyHitRate.hit(bullet.getBullet().getPower());

            enemyHitsDataCount++;
        } else {
            enemyHitRate.hit();
            enemyEnergyHitRate.hit(bullet.getBullet().getPower());
            enemyRawHitRate.hit();
        }
    }

    public void bulletMiss(LXXBullet bullet) {
        if (bullet.getSourceState().getName().equals(tomcat.getName())) {
            myHitRate.miss();
            myEnergyHitRate.miss(bullet.getBullet().getPower());
            myRawHitRate.miss();
        } else {
            enemyHitRate.miss();
            enemyEnergyHitRate.miss(LXXUtils.getBulletPower(bullet.getSpeed()));
            enemyRawHitRate.miss();
        }
    }

    public void bulletIntercepted(LXXBullet bullet) {
        if (bullet.getSourceState().getName().equals(tomcat.getName())) {
            myRawHitRate.miss();
            myEnergyHitRate.miss(bullet.getBullet().getPower());
            enemyHitsDataCount++;
        } else {
            enemyEnergyHitRate.miss(bullet.getBullet().getPower());
            enemyRawHitRate.miss();
        }
    }

    public double getMyRawHitRate() {
        return myRawHitRate.getHitRate();
    }

    public HitRate getEnemyHitRate() {
        return enemyHitRate;
    }

    public void waveBroken(Wave w) {
        enemyWavesDataCount++;
    }
}
