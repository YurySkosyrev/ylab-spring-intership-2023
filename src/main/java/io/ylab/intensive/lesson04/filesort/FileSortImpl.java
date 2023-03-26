package io.ylab.intensive.lesson04.filesort;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import javax.sql.DataSource;

public class FileSortImpl implements FileSorter {
  private DataSource dataSource;

  public FileSortImpl(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public File sort(File data) throws IOException, SQLException {
    // ТУТ ПИШЕМ РЕАЛИЗАЦИЮ

    int BATCH_SIZE = 10000;

    String query = "insert into numbers (val) values (?)" ;

    try (Connection connection = dataSource.getConnection();
         PreparedStatement preparedStatement = connection.prepareStatement(query)) {

      connection.setAutoCommit(false);

      try (FileInputStream fileInputStream = new FileInputStream(data);
           Scanner scanner = new Scanner(fileInputStream)) {

        int currentSize = 0;

        while (scanner.hasNextLong()) {

          String nextString = scanner.next();
          currentSize++;

          preparedStatement.setLong(1, Long.parseLong(nextString));
          preparedStatement.addBatch();

          if (currentSize == BATCH_SIZE) {
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

        if (currentSize > 0) {
          try {
            preparedStatement.executeBatch();
            connection.commit();
          } catch (BatchUpdateException ex) {
            connection.rollback();
            ex.printStackTrace();
          }
        }

        }
      }

    String sortQuery = "select * from numbers order by val desc";
    PrintWriter printWriter = new PrintWriter("sql-with-batch-sorted.txt");
    String cleanQuery = "delete from numbers";

    try(Connection connection = dataSource.getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement(sortQuery);
        PreparedStatement deleteStatement = connection.prepareStatement(cleanQuery)) {

      ResultSet rs = preparedStatement.executeQuery();

      try {
        while (rs.next()) {
          printWriter.println(rs.getString(1));
        }
      } finally {
        printWriter.close();
      }

     deleteStatement.executeUpdate();

    }
    return new File("sql-with-batch-sorted.txt");
  }
}
