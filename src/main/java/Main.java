import com.google.api.client.util.DateTime;
import com.google.api.services.samples.youtube.cmdline.data.Search;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

import static com.google.api.services.samples.youtube.cmdline.data.Search.*;

public class Main {
    //ids - коллекция id всех видео по запросу, data - коллекция для сортировки по дате добавления,
    //new_map - отсортированная коллекция
    private static List<String> ids;
    private static Map<String, Map<String, String>> data;
    private static Map<String, Map<String, String>> new_map;
    private static Map<String,Map<String,Map<String,String>>>comments; //коллекция данных о комментариях к видео

    public static void main(String[] args) throws IOException {

        Search search = new Search(); //делаем запрос
        List<SearchResult> result=search.searchResultList; //получаем результат
        comments=new TreeMap<String, Map<String, Map<String, String>>>();
        ids = new ArrayList<String>();
        data = new HashMap<String, Map<String, String>>();
        //если список не пуст, выводим
        if (result!= null) {
            //1.собираем все id видео из полученного списка для дальнейшего получения данных
            get_video_id(result.iterator(), queryTerm);
            YouTube.Videos.List videosListByIdRequest = youtube.videos().list("id,statistics," +
                    "snippet,recordingDetails");
            //2.получаем всю необходимую информацию
            get_info(videosListByIdRequest,comments,apiKey);
        }
        //создаем результативный .xls
        create_XLS();
    }
    //метод сбора id видео из всего списка = 1 запрос
    private static void get_video_id(Iterator<SearchResult> iteratorSearchResults, String query) {
        if (!iteratorSearchResults.hasNext()) {
            System.out.println(" There aren't any results for your query.");
        }
//перебираем список видео
        while (iteratorSearchResults.hasNext()) {
            SearchResult singleVideo = iteratorSearchResults.next();
            ResourceId rId = singleVideo.getId(); //id видео
            if (rId.getKind().equals("youtube#video")) {
                String id = rId.getVideoId();
                ids.add(id); //здесь берем только id для дальнейшего получения данных
            }
        }
    }

    //2 запрос = сбор всей необходимой информации
    private static void get_info( YouTube.Videos.List videosListByIdRequest,Map<String,
            Map<String,Map<String,String>>>comments,
                                  String apiKey) throws IOException {
        Iterator<String> iter = ids.iterator();
        //обращаемся к каждому видео для получения данных
        while (iter.hasNext()) {
            Map<String,Map<String,String>> comments_data=new TreeMap<String, Map<String, String>>();
            //берем данные о каждом видео
            //  YouTube.Videos.List videosListByIdRequest = youtube.videos().list("id,statistics,snippet,recordingDetails");
            String id = iter.next();
            //в запросе задаем id видео
            videosListByIdRequest.setId(id);
            //API KEY
            videosListByIdRequest.setKey(apiKey);
            //запрашиваем
            VideoListResponse listResponse = videosListByIdRequest.execute();
            //собираем все возможные данные по видео
            Map<String, String> map = getInformation(listResponse);
            //второй запрос - данные об авторе(канале) данного видео
            // YouTube.Channels.List channelsList=youtube.channels().list("statistics,snippet");
            //channelsList.setId(map.get("channel_id"));
            //channelsList.setKey(apiKey);
            //ChannelListResponse listResponse2 = channelsList.execute();
            //getChannelInfo(listResponse2,map);
            //System.out.println(id);
            CommentThreadListResponse videoCommentsListResponse = youtube.commentThreads()
                    .list("id,snippet").setVideoId(id).setTextFormat("plainText")
                    .setMaxResults((long) 100)
                    .execute();
            getVideoComments(videoCommentsListResponse, id,comments_data);
            String nextPage = videoCommentsListResponse.getNextPageToken();

            //перебираем все страницы = следующие запросы
            do {
                videoCommentsListResponse=youtube.commentThreads()
                        .list("id,snippet").setVideoId(id).setTextFormat("plainText")
                        .setPageToken(nextPage)
                        .setMaxResults((long) 100).execute();
                getVideoComments(videoCommentsListResponse, id,comments_data);
                nextPage = videoCommentsListResponse.getNextPageToken();
            }
            while (nextPage!=null);
            //заполняем карту данных для удобства
            data.put(id, map);
            comments.put(id,comments_data); //ДОБАВЛЯЕМ ВСЕ КОММЕНТАРИИ К ВИДЕО
        }
        //сортировка видео по дате выкладывания
        sort(data);
        //запись результата в документ(требовалось ранее)
        //   write(new_map);

        //добавление данных о видео в БД
        load_data("postgres","qwerty",new_map,false,null);
        //загружаем комментарии на базу данных: для каждого видео - отдельный запрос
        for (Map.Entry<String, Map<String, Map<String,String>>> entry : comments.entrySet()) {
            load_data("postgres", "qwerty", entry.getValue(), true,entry.getKey());
        }
        //создание таблиц nodes - вершины, edges - дуги
        create_XLS();
    }
    //сбор информации о видео в коллекцию
    private static Map<String, String> getInformation(VideoListResponse listResponse) {

        Video current_video = listResponse.getItems().get(0);
        String title = current_video.getSnippet().getTitle(); //название видео
        String author = current_video.getSnippet().get("channelTitle").toString(); //название канала
        DateTime time = current_video.getSnippet().getPublishedAt();//дата публикации
        String date = time.toString().substring(0, 19);
        String tags= "";
        //теги, если есть
        if (current_video.getSnippet().getTags()!=null){
            tags= current_video.getSnippet().getTags().toString();
        }
        //System.out.println(current_video.getContentDetails());
        String description = current_video.getSnippet().getDescription(); //описание видео
        BigInteger view_count = current_video.getStatistics().getViewCount();//число просмотров
        //количество лайков
        BigInteger likes_count = null;
        if (current_video.getStatistics().getLikeCount() != null) {
            likes_count = current_video.getStatistics().getLikeCount();
        }
        //количество дизлайков
        BigInteger dislikes_count = null;
        if (current_video.getStatistics().getDislikeCount() != null) {
            dislikes_count = current_video.getStatistics().getDislikeCount();
        }
        //количество комментариев
        BigInteger comments_count = null;
        if (current_video.getStatistics().getCommentCount() != null) {
            comments_count = current_video.getStatistics().getCommentCount();
        }
//заносим данные в карту
        Map<String, String> data = new HashMap<String, String>();
        data.put("title", title);
        data.put("author", author);
        data.put("date", date);
        data.put("description", description);
        data.put("channel_id",current_video.getSnippet().getChannelId());
        if (view_count == null)
            data.put("view_count", "0");
        else
            data.put("view_count", view_count.toString());
        if (likes_count == null)
            data.put("likes_count", "0");
        else
            data.put("likes_count", likes_count.toString());
        if (dislikes_count == null)
            data.put("dislikes_count", "0");
        else
            data.put("dislikes_count", dislikes_count.toString());
        if (comments_count == null)
            data.put("comments_count", "0");
        else
            data.put("comments_count", comments_count.toString());

        data.put("tags",tags);
        return data;
    }
    //сбор информации о канале
    private static void getChannelInfo(ChannelListResponse listResponse,Map<String,String> data) {

        Channel current_channel = listResponse.getItems().get(0);
        BigInteger follower_count=current_channel.getStatistics().getSubscriberCount();
        data.put("follower_count",follower_count.toString());
        BigInteger video_count=current_channel.getStatistics().getVideoCount();
        data.put("video_count",video_count.toString());
        String description="";
        try {
            description= current_channel.getSnippet().getDescription();
        }
        catch (Exception e){
            System.out.println(e);
        }

        data.put("channel_info",description);

    }
    //сбор комментариев
    private static void getVideoComments(CommentThreadListResponse videoCommentsListResponse,
                                         String id,Map <String,Map<String,String>> map){
        List<CommentThread> videoComments = videoCommentsListResponse.getItems();
        //каждый комментарий
        for (CommentThread videoComment : videoComments) {
            Map<String,String> data=new TreeMap<String, String>();
            CommentSnippet snippet = videoComment.getSnippet().getTopLevelComment()
                    .getSnippet();
            data.put("comment_id",videoComment.getId());
            data.put("author",snippet.getAuthorDisplayName());
            data.put("author_id",snippet.getAuthorChannelId().toString());
            data.put("comment",snippet.getTextDisplay());
            data.put("date",snippet.getPublishedAt().toString());
            map.put(videoComment.getId(),data);
        }
    }
    //сортировка видео по дате выкладывания (быстрая сортировка)
    private static void sort(Map<String, Map<String, String>> data) {

        Date date = null;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        Map<Date, String> data_map = new HashMap<Date, String>();
        new_map = new LinkedHashMap<String, Map<String, String>>();
        Date[] data_list = new Date[data.size()];
        List<String> sort_ids = new LinkedList<String>();
        int i = 0;
        for (Map.Entry<String, Map<String, String>> entry : data.entrySet()) {
            try {
                date = formatter.parse(entry.getValue().get("date")); //в дату
                data_map.put(date, entry.getKey());
                data_list[i] = date;
                i++;
            } catch (Exception e) {
                System.out.println(e);
            }
        }
        /*
//дата до
        for (int j = 0; j < data_list.length; j++) {
            System.out.println(data_list[j]);
        }
        */
        //сортировка дат
        quickSort(data_list, 0, data_list.length - 1);
        for (int j = 0; j < data_list.length; j++) {
            //    System.out.println(data_list[j]);
        }
        for (int j = 0; j < data_list.length; j++) {
            System.out.println(data_list[j]);
            Map<String, String> current_data = data.get(data_map.get(data_list[j]));
            new_map.put(data_map.get(data_list[j]), current_data);
        }

        /*
        //дата после
        for (Map.Entry<String, Map<String, String>> entry : new_map.entrySet()) {
            System.out.println(entry.getKey() + entry.getValue().get("date"));
        }
        */
    }
    private static void quickSort(Date[] array, int low, int high) {
        if (array.length == 0)
            return;//завершить выполнение если длина массива равна 0

        if (low >= high)
            return;//завершить выполнение если уже нечего делить

        //1.выбрать опорный элемент
        int middle = low + (high - low) / 2;
        Date opora = array[middle];

        //2.разделить на подмассивы, который больше и меньше опорного элемента
        int i = low, j = high;
        while (i <= j) {
            //текущее меньше опорного
            while (array[i].compareTo(opora) == -1) {
                i++;
            }
            //текущее больше опорного
            while (array[j].compareTo(opora) == 1) {
                j--;
            }
//меняем местами элементы
            if (i <= j) {
                Date temp = array[i];
                array[i] = array[j];
                array[j] = temp;
                i++;
                j--;
            }
        }

        // рекурсия для сортировки левой и правой части
        if (low < j)
            quickSort(array, low, j);

        if (high > i)
            quickSort(array, i, high);
    }
    //запись в документ
    private static void write(Map<String, Map<String, String>> from) throws IOException {
        FileWriter writer = new FileWriter("youtube_data.txt");
        for (Map.Entry<String, Map<String, String>> entry : from.entrySet()) {
            writer.write("Видео:" + entry.getKey());
            writer.append('\n');
            for (Map.Entry<String, String> entry2 : entry.getValue().entrySet()) {
                writer.append(entry2.getKey() + ":");
                writer.append(entry2.getValue());
                writer.append('\n');
            }
            writer.append('\n');
            writer.append("=============================================================\n");
        }
        writer.flush();

    }
    //занесение данных в БД POSTGRESQL
    private static void load_data(String user,String password,Map<String, Map<String, String>> from,
                                  boolean comments,String video_id) {
        Connection c;
        Statement stmt;
        double i=Math.random();
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager
                    .getConnection("jdbc:postgresql://localhost:5431/postgres", user, password);
            c.setAutoCommit(false);
            System.out.println("-- Opened database successfully");
            String sql;
            stmt = c.createStatement(); //открываем соединение
            //заполнение таблицы comments
            if(comments){
                for (Map.Entry<String, Map<String, String>> entry : from.entrySet()) {
                    sql="INSERT INTO postgres.public.comments (comment_id, video_id, comment_text, comment_author, comment_date,author_id) VALUES (?,?,?,?,?,?)";
                    PreparedStatement stat = c.prepareStatement(sql);
                    stat.setString(1,entry.getValue().get("comment_id"));
                    stat.setString(2,video_id);
                    stat.setString(3,entry.getValue().get("comment"));
                    stat.setString(4,entry.getValue().get("author"));
                    stat.setString(5,entry.getValue().get("date"));
                    stat.setString(6, entry.getValue().get("author_id"));
                    stat.executeUpdate();
                }
            }
            //заполнение таблицы данных о видео
            else {
                for (Map.Entry<String, Map<String, String>> entry : from.entrySet()) {
                    sql = "INSERT INTO postgres.public.videos (video_id,id,video_title,author,publication_date,description,view_count" +
                            ",likes_count,dislikes_count,comments_count,tags,channel_id,channel_follovers_count," +
                            "channel_video_count,channel_description) VALUES ('" + entry.getKey() + "'," + i + ",'"
                            + entry.getValue().get("title") + "','" + entry.getValue().get("author") + "','"
                            + entry.getValue().get("date") + "','" + entry.getValue().get("description") + "',"
                            + entry.getValue().get("view_count") + "," + entry.getValue().get("likes_count") + ","
                            + entry.getValue().get("dislikes_count") + "," + entry.getValue().get("comments_count") + ",'"
                            + entry.getValue().get("tags") + "' ,'" + entry.getValue().get("channel_id") + "'," +
                            entry.getValue().get("follower_count") + "," + entry.getValue().get("video_count") + ",'" +
                            entry.getValue().get("channel_info") + "')";
                    i++;
                    stmt.executeUpdate(sql); //заносим данные
                }
            }
            stmt.close();
            c.commit();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void create_XLS() throws IOException {
        Connection c;
        Statement stmt;
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet("Edges");

        //sheet.autoSizeColumn(1);
        Cell cell;
        HSSFCellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);

        Row row;
        int rownum = 0;

        int i=0;//id
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager
                    .getConnection("jdbc:postgresql://localhost:5431/postgres","postgres","qwerty");
            c.setAutoCommit(false);
            System.out.println("-- Opened database successfully");
            String sql;
            stmt = c.createStatement(); //открываем соединение

            //создание 1й таблицы Edge
            sql="SELECT channel_id as channel,comment_author,author_id FROM videos JOIN comments on videos.video_id = comments.video_id GROUP BY channel,comment_author,author_id";
            ResultSet rs= stmt.executeQuery(sql);
            int columns = rs.getMetaData().getColumnCount();
            //создание строки
            row = sheet.createRow(rownum);
            cell = row.createCell(1, CellType.STRING);
            cell.setCellValue("target");
            cell.setCellStyle(style);
            cell = row.createCell(2, CellType.STRING);
            cell.setCellValue("comment_author");
            cell.setCellStyle(style);
            cell = row.createCell(3, CellType.STRING);
            cell.setCellValue("who");
            cell.setCellStyle(style);
            rownum++;

            while (rs.next()) {
                row = sheet.createRow(rownum);
                String channel=rs.getString("channel");
                String comment_author=rs.getString("comment_author");
                String author_id=rs.getString("author_id");
                cell = row.createCell(1, CellType.STRING);
                cell.setCellValue(channel);
                cell = row.createCell(2, CellType.STRING);
                cell.setCellValue(comment_author);
                cell = row.createCell(3, CellType.STRING);
                author_id=author_id.substring(7);
                String []res=author_id.split("}");
                author_id=res[0];
                cell.setCellValue(author_id);
                rownum++;
            }
            sheet = workbook.createSheet("Nodes");
            rownum=1;
            row = sheet.createRow(rownum);
            cell = row.createCell(1, CellType.STRING);
            cell.setCellValue("node");
            cell = row.createCell(2, CellType.STRING);
            cell.setCellValue("author");
            rownum++;
            //создание второй таблицы Nodes
            sql="select DISTINCT channel_id,author from videos";
            rs= stmt.executeQuery(sql);

            while (rs.next()) {
                row = sheet.createRow(rownum);
                String channel=rs.getString("channel_id");
                String author=rs.getString("author");
                cell = row.createCell(1, CellType.STRING);
                cell.setCellValue(channel);
                cell = row.createCell(2, CellType.STRING);
                cell.setCellValue(author);
                rownum++;
            }
            //дополнение таблицы вершин(там только авторы видео) авторами комментариев
            sql="select author_id,comment_author from comments";
            rs= stmt.executeQuery(sql);
            while (rs.next()) {
                row = sheet.createRow(rownum);
                String author_id=rs.getString("author_id");
                String comment_author=rs.getString("comment_author");
                cell = row.createCell(1, CellType.STRING);
                author_id=author_id.substring(7);
                String []res=author_id.split("}");
                author_id=res[0];
                cell.setCellValue(author_id);
                cell = row.createCell(2, CellType.STRING);
                cell.setCellValue(comment_author);

                rownum++;
            }
            stmt.close();
            c.commit();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        //
        File file = new File("data.xls");
        //file.getParentFile().mkdirs();

        FileOutputStream outFile = new FileOutputStream(file);
        workbook.write(outFile);
    }
}
