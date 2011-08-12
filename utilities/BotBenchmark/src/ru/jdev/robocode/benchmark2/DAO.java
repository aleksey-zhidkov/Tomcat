/*
 * Copyright (c) 2011 Alexey Zhidkov (Jdev). All Rights Reserved.
 */

package ru.jdev.robocode.benchmark2;

import org.h2.Driver;
import robocode.BattleResults;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DAO {

    private final Connection conn;
    private final PreparedStatement insertChallenge;
    private final PreparedStatement insertBattleResults;

    private final PreparedStatement getResults;

    public DAO() throws SQLException {
        DriverManager.registerDriver(new Driver());
        conn = DriverManager.getConnection("jdbc:h2:file:./database/benchmark", "bb", "bb");

        final Statement statement = conn.createStatement();
        statement.execute("CREATE TABLE IF NOT EXISTS challenges (id BIGINT PRIMARY KEY, challenger_name VARCHAR)");
        statement.execute("CREATE TABLE IF NOT EXISTS battle_results (id int PRIMARY KEY AUTO_INCREMENT" +
                ", challenge_id BIGINT, bot_name VARCHAR, rounds INT, bullet_damage INT, score INT, score_percents DOUBLE, survival_percents DOUBLE)");
        statement.close();

        insertChallenge = conn.prepareStatement("INSERT INTO challenges (id, challenger_name) VALUES (?, ?)");
        insertBattleResults = conn.prepareStatement("INSERT INTO battle_results (" +
                "challenge_id, bot_name, rounds, bullet_damage, score, score_percents, survival_percents) VALUES (?, ?, ?, ?, ?, ?, ?)");

        getResults = conn.prepareStatement("select bot_name, avg(score_percents) avg_score_percents, avg(survival_percents) avg_survival_percents, avg(bullet_damage) avg_bullet_damage, avg(score) avg_score, min(score), max(score) " +
                "from battle_results where challenge_id = ? group by bot_name order by avg_score desc");
    }

    public void storeChallenge(Challenge challenge) throws SQLException {
        insertChallenge.setLong(1, challenge.getStartTime());
        insertChallenge.setString(2, challenge.getChallengerBotName());
        insertChallenge.execute();

        for (BattleResults[] brs : challenge.getBattleResults()) {
            int totalScore = 0;
            for (BattleResults br : brs) {
                totalScore += br.getScore();
            }

            for (BattleResults br : brs) {
                insertBattleResults.setLong(1, challenge.getStartTime());
                insertBattleResults.setString(2, br.getTeamLeaderName());
                // todo(zhidkov): fix me
                insertBattleResults.setInt(3, BotBenchmark2.ROUNDS);
                insertBattleResults.setInt(4, br.getBulletDamage());
                insertBattleResults.setInt(5, br.getScore());
                insertBattleResults.setDouble(6, (double) br.getScore() / totalScore);
                insertBattleResults.setDouble(7, (double) (br.getFirsts()) / BotBenchmark2.ROUNDS);
                insertBattleResults.execute();
            }
        }

    }

    public List<String[]> getResults(long challengeId) throws SQLException {
        getResults.setLong(1, challengeId);
        ResultSet rs = getResults.executeQuery();

        List<String[]> res = new ArrayList<String[]>();
        while (rs.next()) {
            //bot_name, avg_score_percents, avg_survival_percents, avg_bullet_damage, avg_score, min(score), max(score)
            String[] row = new String[7];
            row[0] = rs.getString(1);
            row[1] = String.format("%3.3f", rs.getDouble(2) * 100);
            row[2] = String.format("%3.3f", rs.getDouble(3) * 100);
            row[3] = String.format("%d", rs.getInt(4));
            row[4] = String.format("%d", rs.getInt(5));
            row[5] = rs.getString(6);
            row[6] = rs.getString(7);
            res.add(row);
        }

        return res;
    }

    public void dispose() throws SQLException {
        insertChallenge.close();
        conn.close();
    }

}
