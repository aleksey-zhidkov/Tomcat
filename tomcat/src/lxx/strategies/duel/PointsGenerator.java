/*
 * Copyright (c) 2011 Alexey Zhidkov (Jdev). All Rights Reserved.
 */

package lxx.strategies.duel;

import lxx.LXXRobotState;
import lxx.bullets.LXXBullet;
import lxx.bullets.PastBearingOffset;
import lxx.bullets.enemy.EnemyBulletPredictionData;
import lxx.strategies.MovementDecision;
import lxx.utils.*;
import robocode.Rules;
import robocode.util.Utils;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.*;

public class PointsGenerator {

    private final DistanceController distanceController;
    private final BattleField battleField;

    public PointsGenerator(DistanceController distanceController, BattleField battleField) {
        this.distanceController = distanceController;
        this.battleField = battleField;
    }

    private PointDanger getPointDanger(LXXBullet lxxBullet, LXXPoint robotPos) {
        return new PointDanger(lxxBullet, lxxBullet != null ? getWaveDanger(robotPos, lxxBullet) : 0, 0, battleField.center.aDistance(robotPos));
    }

    private double getWaveDanger(LXXPoint pnt, LXXBullet bullet) {
        final EnemyBulletPredictionData aimPredictionData = (EnemyBulletPredictionData) bullet.getAimPredictionData();
        final List<PastBearingOffset> predictedBearingOffsets = aimPredictionData.getPredictedBearingOffsets();
        if (predictedBearingOffsets.size() == 0) {
            return 0;
        }
        final LXXPoint firePos = bullet.getFirePosition();
        final double alpha = LXXUtils.angle(firePos.x, firePos.y, pnt.x, pnt.y);
        final double bearingOffset = Utils.normalRelativeAngle(alpha - bullet.noBearingOffset());
        final double robotWidthInRadians = LXXUtils.getRobotWidthInRadians(alpha, firePos.aDistance(pnt));

        double bulletsDanger = 0;
        final double hiEffectDist = robotWidthInRadians * 0.75;
        final double lowEffectDist = robotWidthInRadians * 2.55;
        for (PastBearingOffset bo : predictedBearingOffsets) {
            if (bo.bearingOffset < bearingOffset - lowEffectDist) {
                continue;
            } else if (bo.bearingOffset > bearingOffset + lowEffectDist) {
                break;
            }
            final double dist = abs(bearingOffset - bo.bearingOffset);
            if (dist < hiEffectDist) {
                bulletsDanger += (2 - (dist / hiEffectDist)) * bo.danger;
            } else if (dist < lowEffectDist) {
                bulletsDanger += (1 - (dist / lowEffectDist)) * bo.danger;
            }
        }

        double intersection = 0;
        final double halfRobotWidthInRadians = robotWidthInRadians / 2;
        final IntervalDouble robotIval = new IntervalDouble(bearingOffset - halfRobotWidthInRadians, bearingOffset + halfRobotWidthInRadians);
        for (IntervalDouble shadow : bullet.getMergedShadows()) {
            if (robotIval.intersects(shadow)) {
                intersection += robotIval.intersection(shadow);
            }
        }
        bulletsDanger *= 1 - intersection / robotWidthInRadians;

        return bulletsDanger;
    }

    public List<WSPoint> generatePoints(APoint dstPoint, LXXBullet bullet, RobotImage robotImg, RobotImage opponentImg, int time) {
        final List<WSPoint> points = new ArrayList<WSPoint>();

        generatePoints(dstPoint, bullet, robotImg, opponentImg, time, points);

        return points;
    }

    public int generatePoints(APoint dstPoint, LXXBullet bullet, RobotImage robotImg, RobotImage opponentImg, int time, List<WSPoint> points) {

        final LXXPoint surfPoint = getSurfPoint(opponentImg, bullet);
        final double travelledDistance = bullet.getTravelledDistance();
        final APoint firePosition = bullet.getFirePosition();
        final double bulletSpeed = bullet.getSpeed();
        final double enemyDesiredVelocity;
        if (opponentImg != null) {
            enemyDesiredVelocity = Rules.MAX_VELOCITY * signum(opponentImg.getVelocity());
        } else {
            enemyDesiredVelocity = 0;
        }

        do {
            final MovementDecision md = getMovementDecision(surfPoint, dstPoint, robotImg, opponentImg);
            robotImg.apply(md);
            if (opponentImg != null) {
                opponentImg.apply(new MovementDecision(enemyDesiredVelocity, 0));
            }
            if (points != null) {
                points.add(new WSPoint(robotImg, getPointDanger(bullet, robotImg.getPosition())));
                if (opponentImg != null) {
                    for (WSPoint prevPoint : points) {
                        prevPoint.danger.setMinDistToEnemy(prevPoint.aDistance(opponentImg));
                    }
                }
            }
            time++;
        } while (firePosition.aDistance(robotImg) - travelledDistance > bulletSpeed * time);

        return time;
    }

    public int playForwardWaveSuring(APoint dstPoint, LXXBullet bullet, RobotImage robotImg, RobotImage opponentImg) {
        return generatePoints(dstPoint, bullet, robotImg, opponentImg, 0, null);
    }

    public MovementDecision getMovementDecision(LXXPoint surfPoint, APoint dstPoint, LXXRobotState robot, LXXRobotState opponent) {
        final double alphaToRobot = surfPoint.angleTo(robot);
        final double alphaToDst = surfPoint.angleTo(dstPoint);
        final double acceleratedSpeed = min(Rules.MAX_VELOCITY, robot.getSpeed() + Rules.ACCELERATION);
        final double desiredSpeed = (robot.aDistance(surfPoint.project(alphaToDst, surfPoint.aDistance(robot))) > LXXUtils.getStopDistance(acceleratedSpeed) + acceleratedSpeed + 1)
                ? 8
                : 0;

        final OrbitDirection orbitDirection;
        if (Utils.normalRelativeAngle(alphaToDst - alphaToRobot) >= 0) {
            orbitDirection = OrbitDirection.CLOCKWISE;
        } else {
            orbitDirection = OrbitDirection.COUNTER_CLOCKWISE;
        }

        return getMovementDecision(surfPoint, orbitDirection, robot, opponent, desiredSpeed);
    }

    private MovementDecision getMovementDecision(LXXPoint surfPoint, OrbitDirection orbitDirection,
                                                 LXXRobotState robot, LXXRobotState opponent, double desiredSpeed) {
        final LXXPoint robotPos = robot.getPosition();
        double desiredHeading = distanceController.getDesiredHeading(surfPoint, robotPos, orbitDirection);
        if (robotPos.x < battleField.noSmoothX.a || robotPos.x > battleField.noSmoothX.b ||
                robotPos.y < battleField.noSmoothY.a || robotPos.y > battleField.noSmoothY.b) {
            desiredHeading = battleField.smoothWalls(robotPos, desiredHeading, orbitDirection == OrbitDirection.CLOCKWISE);
        }
        if (opponent != null) {
            final LXXPoint oppPos = opponent.getPosition();
            double angleToOpponent = LXXUtils.angle(robotPos.x, robotPos.y, oppPos.x, oppPos.y);
            if (((LXXUtils.anglesDiff(desiredHeading, angleToOpponent) < LXXUtils.getRobotWidthInRadians(angleToOpponent, robot.aDistance(opponent)) * 1.2))) {
                desiredSpeed = 0;
            }
        }

        return MovementDecision.toMovementDecision(robot, desiredSpeed, desiredHeading);
    }

    public LXXPoint getSurfPoint(LXXRobotState duelOpponent, LXXBullet bullet) {
        if (duelOpponent == null) {
            return bullet.getFirePosition();
        }

        return new LXXPoint(duelOpponent);
    }

}
