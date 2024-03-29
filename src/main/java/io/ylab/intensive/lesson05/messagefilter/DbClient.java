package io.ylab.intensive.lesson05.messagefilter;

import io.ylab.intensive.lesson05.DbUtil;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

@Component
public class DbClient {

    public DbClient(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private final DataSource dataSource;

    public void init() {

        String deleteQuery = "delete from bad_words";

        String ddl = "create table bad_words (word varchar)";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(deleteQuery)) {
            DatabaseMetaData databaseMetaData = connection.getMetaData();

            ResultSet rs = databaseMetaData.getTables(null, null, "bad_words", new String[]{"TABLE"});
            if (rs.next()) {
                preparedStatement.executeUpdate();
            } else {
                DbUtil.applyDdl(ddl, dataSource);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void load(File file) {

        int BATCH_SIZE = 50;
        String insertQuery = "insert into bad_words (word) values (?)";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {

            connection.setAutoCommit(false);

            try (FileInputStream fileInputStream = new FileInputStream(file);
                 Scanner scanner = new Scanner(fileInputStream)) {

                int currentSize = 0;

                while (scanner.hasNextLine()) {

                    String word = scanner.nextLine().toLowerCase();
                    currentSize++;

                    preparedStatement.setString(1, word);
                    preparedStatement.addBatch();

                    if (currentSize == BATCH_SIZE || (!scanner.hasNext() && currentSize > 0)) {
                        try {
                            preparedStatement.executeBatch();
                            connection.commit();
                        } catch (BatchUpdateException ex) {
                            connection.rollback();
                            ex.printStackTrace();
                        }

                        currentSize = 0;
                    }
                }
            }
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    public Set<String> isWordInDB(Set<String> words) {

        String query = "select word from bad_words where word=?";
        Set<String> existWords = new HashSet<>();

        try(Connection connection = dataSource.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            for (String word : words) {
                preparedStatement.setString(1, word.toLowerCase());
                ResultSet rs = preparedStatement.executeQuery();

                if (rs.next()) {
                    existWords.add(word);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return existWords;
    }
}
