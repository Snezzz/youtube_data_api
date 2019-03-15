import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;

/*В данном классе производятся запросы к БД для анализа полученных данных(статистика)*/
public class Results {
    Connection c;
    FileOutputStream outFile;
    HSSFWorkbook workbook;
    Results() throws FileNotFoundException {
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager
                    .getConnection("jdbc:postgresql://localhost:5431/postgres", "postgres", "qwerty");
            c.setAutoCommit(false);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        File file = new File("analysis_result.xls");
        outFile = new FileOutputStream(file);
        workbook = new HSSFWorkbook();

    }

    public void get(String sql,String name) throws SQLException, IOException {
        Statement stmt;
        stmt = c.createStatement(); //открываем соединение
        ResultSet resultSet = stmt.executeQuery(sql);
        set(resultSet,workbook,name);
    }
    public void set(ResultSet rs,HSSFWorkbook workbook,String name) throws SQLException {
        HSSFSheet sheet = workbook.createSheet(name);
        Cell cell;
        HSSFCellStyle style = workbook.createCellStyle();
        style.setAlignment(
                HorizontalAlignment.CENTER);

        Row row;
        int rownum = 0;
        int columns = rs.getMetaData().getColumnCount();
        row = sheet.createRow(rownum);
        //заголовки
        for(int i=1;i<=columns;i++) {
            sheet.autoSizeColumn(i);
            cell = row.createCell(i, CellType.STRING);
            cell.setCellValue(rs.getMetaData().getColumnName(i));
        }
        rownum++;
        //данные
        while(rs.next()){
            row = sheet.createRow(rownum);
            for(int i=1;i<=columns;i++) {
                cell = row.createCell(i, CellType.STRING);
                cell.setCellValue(rs.getString(i));
            }
            rownum++;
        }
    }
    public void put() throws IOException {
        workbook.write(outFile); //запись в файл
    }
}

