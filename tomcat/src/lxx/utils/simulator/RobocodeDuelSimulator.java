/*
 * Copyright (c) 2011 Alexey Zhidkov (Jdev). All Rights Reserved.
 */

package lxx.utils.simulator;

import lxx.LXXRobot;
import lxx.LXXRobotState;
import lxx.Tomcat;
import lxx.bullets.BulletStub;
import lxx.bullets.LXXBullet;
import lxx.strategies.MovementDecision;
import lxx.targeting.Target;
import lxx.ts_log.TurnSnapshot;
import lxx.ts_log.attributes.Attribute;
import lxx.ts_log.attributes.AttributesManager;
import lxx.utils.*;
import robocode.Bullet;
import robocode.Rules;
import robocode.util.Utils;

import java.util.*;

import static java.lang.Math.*;

/**
 * User: jdev
 * Date: 24.02.2011
 */
public class RobocodeDuelSimulator {

    private final Map<RobotProxy, MovementDecision> movementDecisions = new HashMap<RobotProxy, MovementDecision>();
    private final Set<Attribute> attributesToSimulate = new HashSet<Attribute>();

    private final RobotProxy enemyProxy;
    private final RobotProxy meProxy;
    private final long time;
    private final int round;
    private final List<LXXBullet> myBullets;
    private final List<LXXBullet> enemyBullets;

    private long timeElapsed = 0;

    public RobocodeDuelSimulator(Target enemy, Tomcat robot, long time, int round,
                                 Attribute[] attributesToSimulate,
                                 List<LXXBullet> myBullets, List<LXXBullet> enemyBullets) {
        if (enemy != null) {
            this.enemyProxy = new RobotProxy(enemy, time, robot.getGunCoolingRate());
        } else {
            this.enemyProxy = null;
        }
        this.meProxy = new RobotProxy(robot, time, robot.getGunCoolingRate());
        this.time = time;
        this.round = round;

        this.attributesToSimulate.addAll(Arrays.asList(attributesToSimulate));
        this.attributesToSimulate.add(AttributesManager.enemyVelocity);
        this.attributesToSimulate.add(AttributesManager.enemyAcceleration);
        this.attributesToSimulate.add(AttributesManager.enemyTurnRate);

        this.myBullets = new ArrayList<LXXBullet>();
        for (LXXBullet bullet : myBullets) {
            this.myBullets.add(new LXXBullet(bullet.getBullet(), bullet.getWave(), bullet.getAimPredictionData()));
        }

        this.enemyBullets = new ArrayList<LXXBullet>();
        for (LXXBullet bullet : enemyBullets) {
            this.enemyBullets.add(new LXXBullet(bullet.getBullet(), bullet.getWave(), bullet.getAimPredictionData()));
        }
    }

    public void setEnemyMovementDecision(MovementDecision movementDecision) {
        movementDecisions.put(enemyProxy, movementDecision);
    }

    public void setMyMovementDecision(MovementDecision myMovementDecision) {
        movementDecisions.put(meProxy, myMovementDecision);
    }

    public void doTurn() {
        for (RobotProxy proxy : movementDecisions.keySet()) {
            final MovementDecision md = movementDecisions.get(proxy);
            apply(proxy, md);
        }
        processBullets(myBullets);
        processBullets(enemyBullets);
        timeElapsed++;
    }

    private void processBullets(List<LXXBullet> bullets) {
        final List<LXXBullet> toRemove = new ArrayList<LXXBullet>();
        for (LXXBullet bullet : bullets) {
            final Bullet oldBulletState = bullet.getBullet();
            final LXXPoint newBulletPos = bullet.getCurrentPosition().project(bullet.getHeadingRadians(), bullet.getSpeed());
            if (bullet.getFirePosition().aDistance(newBulletPos) > bullet.getFirePosition().aDistance(bullet.getTarget())) {
                toRemove.add(bullet);
            }
            final Bullet newBulletState = new BulletStub(oldBulletState.getHeadingRadians(), newBulletPos.x, newBulletPos.y, oldBulletState.getPower(),
                    oldBulletState.getName(), oldBulletState.getVictim(), true, -2);
            bullet.setBullet(newBulletState);
        }

        bullets.removeAll(toRemove);
    }

    private void apply(RobotProxy proxy, MovementDecision movementDecision) {
        final LXXRobotState state = proxy.getState();
        final double newHeading = Utils.normalAbsoluteAngle(state.getHeadingRadians() + movementDecision.getTurnRateRadians());
        final double acceleration;
        double newVelocity = proxy.getState().getVelocity();
        if (abs(signum(newVelocity) - signum(movementDecision.getDesiredVelocity())) <= 1) {
            acceleration = LXXUtils.limit(-Rules.DECELERATION, abs(movementDecision.getDesiredVelocity()) - abs(newVelocity), Rules.ACCELERATION);
            newVelocity = (abs(newVelocity) + acceleration) * signum(movementDecision.getDesiredVelocity());
        } else {
            // robocode has difficult 2-step rules in this case,
            // but we will keep it simple
            if (abs(newVelocity) > Rules.DECELERATION) {
                newVelocity -= Rules.DECELERATION * signum(newVelocity);
            } else {
                newVelocity = 0;
            }
        }

        double distanceToWall = max(new LXXPoint(state).distanceToWall(state.getBattleField(), state.getAbsoluteHeadingRadians()), 0);
        APoint newPosition;
        if (distanceToWall > newVelocity) {
            newPosition = proxy.getState().project(newVelocity >= 0 ? newHeading : Utils.normalAbsoluteAngle(newHeading + LXXConstants.RADIANS_180), abs(newVelocity));
        } else {
            do {
                newPosition = proxy.getState().project(newVelocity >= 0 ? newHeading : Utils.normalAbsoluteAngle(newHeading + LXXConstants.RADIANS_180), distanceToWall);
                distanceToWall -= 1;
            } while (!proxy.getState().getBattleField().contains(newPosition) && distanceToWall > 0);
            newVelocity = 0;
        }
        proxy.doTurn(new RobotImage(newPosition, newVelocity, newHeading, state.getBattleField(), movementDecision.getTurnRateRadians(), state.getEnergy()));
    }

    public TurnSnapshot getSimulatorSnapshot() {
        final double[] avs = new double[AttributesManager.attributesCount()];

        for (Attribute a : attributesToSimulate) {
            avs[a.getId()] = a.getExtractor().getAttributeValue(enemyProxy, meProxy, myBullets);
        }

        return new TurnSnapshot(avs, time + timeElapsed, round, enemyProxy.getName());
    }

    public RobotProxy getEnemyProxy() {
        return enemyProxy;
    }

    public LXXRobot getMyProxy() {
        return meProxy;
    }

    public List<LXXBullet> getEnemyBullets() {
        return enemyBullets;
    }
}