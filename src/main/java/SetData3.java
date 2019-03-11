import com.google.api.client.util.DateTime;
import com.google.api.client.util.Joiner;
import com.google.api.services.samples.youtube.cmdline.data.ChannelVideo;
import com.google.api.services.samples.youtube.cmdline.data.Search;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import com.google.gson.Gson;
import org.apache.commons.collections4.list.TreeList;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.gephi.preview.api.G2DTarget;
import org.gephi.project.api.ProjectController;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



//import com.google.api.services.samples.youtube.cmdline.data.Search;

public class SetData3 {

    //ids - коллекция id всех видео по запросу, data - коллекция для сортировки по дате добавления,
    //new_map - отсортированная коллекция
    private static List<String> ids;
    private static Map<String, Map<String, String>> data;
    private static Map<String, Map<String, String>> new_map;
    private static Map<String, Map<String, Map<String, String>>> comments; //коллекция данных о комментариях к видео
    private static Map<String,List<String>> nodes;
    private static String comments_count;
    private static Map<String, Map<String, String>> comments_data;
    private static List<CommentThread> videoComments;
    private static List<CommentThread> syncList;
    private static CommentThread videoComment;
    private static int all_comments_count=0;
    public static Map<String, Map<String, Map<String, String>>> nodes_map; //коллекция данных о комментариях к видео
    private static  Map <String, Map<String,String>> main_nodes_map;
    public static String current_author_id;
    public static boolean stopYear = false;
    public static boolean startYear = false;
    static int count = 0;
    public static Map<String,Integer> comment_count;
    private static Connection c;
   // private static G2DTarget target1,target2;
    private static int col=0;
    private static boolean cont=false;
    public static G2DTarget target;

    private static  List tags_away;
    public static ProjectController pc;

    public static void run() throws IOException, ParserConfigurationException, XMLStreamException, SQLException, InterruptedException {

        nodes=new LinkedHashMap<String, List<String>>();
        comment_count=new HashMap<String, Integer>();

        //1.сбор id видео с каналов
        ChannelVideo channel=new ChannelVideo();
        double time_before= System.currentTimeMillis();
        //List<SearchResult> result = search.searchResultList; //получаем результат


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
        //если список не пуст, выводим
        //if (result != null) {
            //1.собираем все id видео из полученного списка для дальнейшего получения данных
          //  get_video_id(channel.video.iterator());

        //для каждого канала
        YouTube.Videos.List videosListByIdRequest = ChannelVideo.youtube.videos().list("id,statistics," +
                "snippet,recordingDetails");
        double in = System.currentTimeMillis();
        for(Map.Entry<String,List<String>> entry : channel.video.entrySet()) {
            System.out.println("канал"+ entry.getKey());
            comments = new HashMap<String, Map<String, Map<String, String>>>();
            ids = new ArrayList<String>();
            data = new HashMap<String, Map<String, String>>();
            nodes_map=new HashMap<String, Map<String, Map<String, String>>>();
            stopYear = false;
            //2.получаем всю необходимую информацию о видео (список)
            get_info( videosListByIdRequest,comments, Search.apiKey,entry.getValue());

        }

        double time_after=System.currentTimeMillis();
        double result_time=(time_after-time_before)/1000;
        System.out.println("Время выполнения запросов:"+result_time+" c.");
        //создаем результативный .xls
       // create_XLS();

        System.out.println("Количество видео:" + channel.max_results);
        System.out.println("Общее количество комментариев:" + all_comments_count);
        System.out.println("Время выполнения программы с построением графа:"+(System.currentTimeMillis()-time_before)/1000);

    }

    //метод сбора id видео из всего списка = 1 запрос
    private static void get_video_id(Iterator<SearchResult> iteratorSearchResults) {
        if (!iteratorSearchResults.hasNext()) {
         //   System.out.println(" There aren't any results for your query.");
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
    private static void get_info(YouTube.Videos.List videosListByIdRequest, Map<String,
            Map<String, Map<String, String>>> comments,
                                 String apiKey, List<String> video) throws IOException, XMLStreamException, SQLException, InterruptedException {
        //     Iterator<String> iter = video.iterator(); //перебираем id
        int video_col = 0;
        Joiner stringJoiner = Joiner.on(',');
        String videoId = stringJoiner.join(video);
        Iterator iter = video.iterator();

        //обращаемся к каждому видео для получения данных
        while (iter.hasNext()) {
            count = 0;
            startYear = false;
            String id = iter.next().toString();

            //видео уже есть в БД
            if (isPresent(id))
                continue;
            // out.writeStartElement("ID");
           comments_data = new TreeMap<String, Map<String, String>>();
            //берем данные о каждом видео

            //в запросе задаем id видео
            videosListByIdRequest.setId(id);
            //API KEY
            videosListByIdRequest.setKey(apiKey);
            //запрашиваем
            VideoListResponse listResponse = videosListByIdRequest.execute();

            //собираем все возможные данные по видео
            Map<String, String> map = getInformation(listResponse);
            //if(startYear){
              //  continue;
            //}
            //2017 год
           //else if(stopYear) {
             //   System.out.println("stopped");
               // break;
           // }

            //исключаем прямые эфиры
            if(map==null){
                System.out.println("прямой эфир");
            }
            if (map != null){
                test(map.get("title"));



                //автор данного видео
                current_author_id = map.get("channel_id");
                main_nodes_map = new HashMap<String, Map<String, String>>();

                comments_count = map.get("comments_count");
                System.out.println("количество комментариев:"+comments_count);
                if (!map.get("comments_count").equals("0")) {
                    // out.writeStartElement("Comments");
                    // out.writeEndElement();
                    String nextPage = "";
                    //перебираем все страницы = следующие запросы
                    do {
                        CommentThreadListResponse videoCommentsListResponse = ChannelVideo.youtube.commentThreads()
                                .list("id,snippet").setVideoId(id).setTextFormat("plainText")
                                .setMaxResults((long) 100).setPageToken(nextPage)
                                .execute();

                        getVideoComments(videoCommentsListResponse, id, comments_data, comments, nodes_map, main_nodes_map);
                        nextPage = videoCommentsListResponse.getNextPageToken();
                    }
                    while ((nextPage != null)||(count<=400)); //не берем более 1000 комментариев
                    //заполняем карту данных для удобства
                    //ключ - тот, на кого ссылаются, значение - кто ссылается
                    data.put(id, map);

                    comments.put(id, comments_data); //ДОБАВЛЯЕМ ВСЕ КОММЕНТАРИИ К ВИДЕО

                } else {

                }

            }
            if(video_col==5){
                sort(data);
                load_data("postgres", "qwerty", new_map, false, null);
                for (Map.Entry<String, Map<String, Map<String, String>>> entry : comments.entrySet()) {
                    optimal_load_data("postgres", "qwerty", entry.getValue(), true, entry.getKey());
                    System.out.println("Комментарии видео "+entry.getKey()+" отправлены");
                }
                System.out.println("success!");
                data = new HashMap<String, Map<String, String>>();
                comments = new HashMap<String, Map<String, Map<String, String>>>();
                video_col=0;
            }
            else
                video_col++;

        }

        System.out.println("количество обработанного видео:"+video_col);
        System.out.println(comments.size());

        //сортировка видео по дате выкладывания
         sort(data);

        //запись результата в документ(требовалось ранее)
        //   write(new_map);

        //добавление данных о видео в БД
      load_data("postgres", "qwerty", new_map, false, null);
        //загружаем комментарии на базу данных: для каждого видео - отдельный запрос
        FileWriter writer = new FileWriter("example.json");
        //writer.write("{");
        Map <String,Integer> video_comments;
        for (Map.Entry<String, Map<String, Map<String, String>>> entry : comments.entrySet()) {
            optimal_load_data("postgres", "qwerty", entry.getValue(), true, entry.getKey());
          //  video_comments=new HashMap<String, Integer>();
          //  get_comments_count("postgres", "qwerty",entry.getKey(),video_comments);
           // create_json(writer,entry.getValue());
        }
        System.out.println("end");
    //    writer.write("}");
       // writer.close();


    }
        //сбор информации о видео в коллекцию
   private static Map<String, String> getInformation(VideoListResponse listResponse) {
     //   private static Map<String, String> getInformation( Video current_video) {

        //System.out.println(current_video.getId());
       Video current_video = listResponse.getItems().get(0);
       System.out.println("Текущее видео:"+current_video.getId());
        if(current_video.getSnippet().getLiveBroadcastContent().equals("none")) {

            String title = current_video.getSnippet().getTitle(); //название видео
            String author = current_video.getSnippet().get("channelTitle").toString(); //название канала
            DateTime time = current_video.getSnippet().getPublishedAt();//дата публикации
            String date = time.toString().substring(0, 19);
            //не последние года
            if(!date.contains("2018")){
                if(date.contains("2019")){
                    startYear = true;
                }
                else if(date.contains("2017")){
                    stopYear = true;
                }
              //System.out.println("остановились на этом видео:"+title);

            }

            String tags = "";
            tags_away = new TreeList();
            //теги, если есть
            if (current_video.getSnippet().getTags() != null) {
                tags = current_video.getSnippet().getTags().toString();
                tags_away = current_video.getSnippet().getTags();
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
            data.put("channel_id", current_video.getSnippet().getChannelId());
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

            data.put("tags", tags);
            return data;
        }
        else return null;
    }

    //сбор информации о канале
    private static void getChannelInfo(ChannelListResponse listResponse, Map<String, String> data) {

        Channel current_channel = listResponse.getItems().get(0);
        String status=current_channel.getStatus().getLongUploadsStatus();
        boolean result_status=false;
        if(status.equals("allowed")||(status.equals("eligible"))){
            result_status=true;
        }
        String country=current_channel.getBrandingSettings().getChannel().getCountry();
        data.put("country",country);
        data.put("confirmed",String.valueOf(result_status));
        /*BigInteger follower_count = current_channel.getStatistics().getSubscriberCount();
        data.put("follower_count", follower_count.toString());
        BigInteger video_count = current_channel.getStatistics().getVideoCount();
        data.put("video_count", video_count.toString());
        String description = "";
        try {
            description = current_channel.getSnippet().getDescription();
        } catch (Exception e) {
            System.out.println(e);
        }

        data.put("channel_info", description);
*/
    }

    //сбор комментариев
    private static void getVideoComments(CommentThreadListResponse videoCommentsListResponse,
                                         String id, Map<String, Map<String, String>> map,
                                         Map<String,Map<String, Map<String, String>>> comments,
                                         Map<String,Map<String, Map<String, String>>> nodes,
                                         Map <String, Map<String,String>> nodes_map) throws IOException, XMLStreamException, InterruptedException {
        videoComments = videoCommentsListResponse.getItems();
        Queue<CommentThread > comment = new ArrayDeque<CommentThread >();
        //        //каждый комментарий
        cont = false;
        syncList = Collections.synchronizedList(videoComments);
        count += videoComments.size();
        if (videoComments.size() > 0) {

                //List<CommentThread> syncList = Collections.synchronizedList(videoComments);
                for (int i = 0; i < 50; i++) {
                    Thread myThread = new Thread(new MyThread(), "Поток" + i);
                    myThread.start();
                }
                Thread.currentThread().join(1000);
                while (!cont) {
                    Thread.currentThread().join(1000);
                }

            find(nodes,current_author_id,nodes_map);
        }
        //не нашли комментарии
        else{

        }

    }
//ответы на комментарии
    private static void get_replies(CommentSnippet snippet,
                                    String video_id,
                                    String comment_author_id,
                                    String id,
                                    Map<String,Map<String,Map<String,String>>> nodes,
                                         Map<String, Map<String, String>> map)
            throws IOException, XMLStreamException {

        String nextPage = "";
        do{
        CommentListResponse commentsListResponse = ChannelVideo.youtube.comments().list("snippet")
                .setParentId(id).setPageToken(nextPage).execute();
        nextPage = commentsListResponse.getNextPageToken();
        List<Comment> comments = commentsListResponse.getItems();

        //Map <String,Map<String,String>> data=new TreeMap<String, Map<String, String>>();
       Map <String,String> information;
       // Map <String,String> connection=new TreeMap<String,String>();
        Map<String,Map <String,String>> help_data=new HashMap<String, Map<String, String>>();
       // List <String> ids= new TreeList<String>();

        String parent_id=id;
        //String parent_id=comment_author_id;
        if (comments.isEmpty()) {
           // System.out.println("There aren't comment replies.");
        } else {
//все ответы
            //out.writeStartElement("Answers");
            long time = System.currentTimeMillis();
            for (Comment commentReply : comments) {

                information = new HashMap<String, String>();
                snippet = commentReply.getSnippet();
                String data_id = snippet.getAuthorChannelId().toString().substring(7);
                String author_id = data_id.substring(0, data_id.length() - 1);

                //заполнение карты comments
                information.put("video_id", video_id);
                information.put("comment_id", commentReply.getId());
                information.put("author", snippet.getAuthorDisplayName());
                information.put("author_id", author_id);
                information.put("parent_id", comment_author_id);
                information.put("comment", preparing(snippet.getTextDisplay()));
                information.put("date", snippet.getPublishedAt().toString());
                information.put("language", preparing(snippet.getTextOriginal()));  //добавляем к главным комментариям
                map.put(commentReply.getId(), information); //в сам комментарий
                //очистка
                information = new HashMap<String, String>();

                //данные о вершине
                information.put("author_id", author_id);
                information.put("author_name", snippet.getAuthorDisplayName());
                information.put("parent_id", snippet.getParentId());
                information.put("video_id", video_id);
                //id автора - информация
                help_data.put(author_id, information);
                //дополняем главную коллекцию

            }
        }
            //если уже был упомянут данный пользователь(повторный ответ)
            find(nodes,comment_author_id,help_data);

        }
        while(nextPage!=null);
       // System.out.println(System.currentTimeMillis());
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


        //сортировка дат
        quickSort(data_list, 0, data_list.length - 1);

        for (int j = 0; j < data_list.length; j++) {
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
//быстрая сортировка
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
    private static void load_data(String user, String password, Map<String, Map<String, String>> from,
                                  boolean comments, String video_id) {
        Statement stmt;
        double i = Math.random();
        try {
          //  Class.forName("org.postgresql.Driver");
            //c = DriverManager
              //      .getConnection("jdbc:postgresql://localhost:5431/postgres", user, password);
            //c.setAutoCommit(false);

            String sql;
            stmt = c.createStatement(); //открываем соединение
            //заполнение таблицы comments
            if (comments) {
                boolean answer = false;
                for (Map.Entry<String, Map<String, String>> entry : from.entrySet()) {
                    sql = "INSERT INTO postgres.public.comments (comment_id, video_id, comment_text, comment_author, " +
                            "comment_date,author_id,parent_id,real_text,answer) VALUES (?,?,?,?,?,?,?,?,?)";
                    PreparedStatement stat = c.prepareStatement(sql);
                    stat.setString(1, entry.getValue().get("comment_id"));
                    stat.setString(2, video_id);
                    stat.setString(3, entry.getValue().get("comment"));
                    stat.setString(4, entry.getValue().get("author"));
                    stat.setString(5, entry.getValue().get("date"));
                    stat.setString(6, entry.getValue().get("author_id"));
                    stat.setString(7, entry.getValue().get("parent_id"));
                    stat.setString(8, entry.getValue().get("language"));
                    if(entry.getValue().get("comment_id").contains("."))
                        answer = true;
                    else
                        answer = false;
                    stat.setBoolean(9, answer);
                    stat.executeUpdate();
                }
            }
            //заполнение таблицы данных о видео
            else {
                for (Map.Entry<String, Map<String, String>> entry : from.entrySet()) {

                    sql="INSERT INTO postgres.public.videos " +
                                    "(video_id,video_title,author,publication_date,description,view_count," +
                            "likes_count,dislikes_count,comments_count,tags,channel_id,channel_follovers_count," +
                            "channel_video_count,channel_description, confirmation) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
                    PreparedStatement  preparedStatement = c.prepareStatement(sql);
                    preparedStatement.setString(1,entry.getKey());
                    preparedStatement.setString(2, entry.getValue().get("title"));
                    preparedStatement.setString(3,entry.getValue().get("author"));
                    preparedStatement.setString(4,entry.getValue().get("date"));
                    preparedStatement.setString(5,entry.getValue().get("description"));
                    preparedStatement.setInt(6,Integer.valueOf(entry.getValue().get("view_count")));
                    preparedStatement.setInt(7,Integer.valueOf(entry.getValue().get("likes_count")));
                    preparedStatement.setInt(8,Integer.valueOf(entry.getValue().get("dislikes_count")));
                    preparedStatement.setInt(9,Integer.valueOf(entry.getValue().get("comments_count")));
                    preparedStatement.setString(10,entry.getValue().get("tags"));
                    preparedStatement.setString(11,entry.getValue().get("channel_id"));
                    preparedStatement.setInt(12,0);
                    preparedStatement.setInt(13,0);
                    preparedStatement.setString(14,entry.getValue().get("channel_info"));
                    preparedStatement.setBoolean(15,Boolean.valueOf(entry.getValue().get("confirmed")));
                    i++;
                    preparedStatement.executeUpdate();
                   // System.out.println("-- Opened database successfully");
                }
            }
            stmt.close();
            c.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
//запись информации о комментаторах в json
    private static void create_json(FileWriter writer, Map<String, Map<String, String>> map) {
    Statement stmt;
    try {

        System.out.println("-- Opened database successfully");
        String sql;
        stmt = c.createStatement(); //открываем соединение
        sql="select * from comments";
        Gson gson = new Gson();

        ResultSet rs = stmt.executeQuery(sql);
        Map<String,String> data_map =new TreeMap<String, String>();
        List<String> result=new LinkedList<String>();
        //JSONWriter json=new JSONStringer();
        while (rs.next()){

            data_map.put("video_id",rs.getString("video_id"));
            String id=rs.getString("author_id");
            String  convert=id.substring(7);
            String author_id=convert.substring(0,convert.length()-1);
            data_map.put("author_id",rs.getString("author_id"));
            data_map.put("comment_author",rs.getString("comment_author"));
            data_map.put("comment_text",rs.getString("comment_text"));
            data_map.put("comment_date",rs.getString("comment_date"));
            result.add('"'+author_id+'"'+":"+gson.toJson(data_map));
            result.add(",");
        }
        for(int i=0;i<result.size();i++) {
            writer.append(result.get(i));
        }


    } catch (SQLException e) {
        e.printStackTrace();
    } catch (IOException e) {
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
        style.setAlignment(
                HorizontalAlignment.CENTER);

        Row row;
        int rownum = 0;

        int i = 0;//id
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager
                    .getConnection("jdbc:postgresql://localhost:5431/postgres", "postgres", "qwerty");
            c.setAutoCommit(false);
            System.out.println("-- Opened database successfully");
            String sql;
            stmt = c.createStatement(); //открываем соединение

            //создание 1й таблицы Edge
            sql = "SELECT channel_id as channel,comment_author,author_id FROM videos JOIN comments on" +
                    " videos.video_id = comments.video_id order by channel_id";
            ResultSet rs = stmt.executeQuery(sql);
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
            List channels=new LinkedList();
            nodes=new HashMap<String, List<String>>();
            /*
            перебираем все ряды
            1.Если мы еще не рассматривали данную вершину, создаем для нее новый список ссылающихся
            Если рассматривали, заполняем предыдущий set смежных вершин
             2.обновляем карту
*/
            String prev="";
            while (rs.next()) {

                row = sheet.createRow(rownum);
                String channel = rs.getString("channel");

                String comment_author = rs.getString("comment_author");
                String author_id = rs.getString("author_id");

                cell = row.createCell(1, CellType.STRING);
                cell.setCellValue(channel);
                cell = row.createCell(2, CellType.STRING);
                cell.setCellValue(comment_author);
                cell = row.createCell(3, CellType.STRING);
                //author_id = author_id.substring(7);
                //String[] res = author_id.split("}");
                //author_id = res[0];
                cell.setCellValue(author_id);
                rownum++;
                //перешли на другое видео
                if((!channel.equals(prev))&&(!prev.equals(""))){
                    nodes.put(prev,channels);
                    channels=new LinkedList();
                }
                channels.add(author_id);
                prev=channel;
            }
            //последнее видео
            nodes.put(prev,channels);

            sheet = workbook.createSheet("Nodes");
            rownum = 1;
            row = sheet.createRow(rownum);
            cell = row.createCell(1, CellType.STRING);
            cell.setCellValue("node");
            cell = row.createCell(2, CellType.STRING);
            cell.setCellValue("author");
            rownum++;
            //создание второй таблицы Nodes
            sql = "select DISTINCT channel_id,author from videos";
            rs = stmt.executeQuery(sql);

            while (rs.next()) {
                row = sheet.createRow(rownum);
                String channel = rs.getString("channel_id");
                String author = rs.getString("author");
                cell = row.createCell(1, CellType.STRING);
                cell.setCellValue(channel);
                cell = row.createCell(2, CellType.STRING);
                cell.setCellValue(author);
                rownum++;
            }
            //дополнение таблицы вершин(там только авторы видео) авторами комментариев
            sql = "select author_id,comment_author from comments";
            rs = stmt.executeQuery(sql);
            while (rs.next()) {
                row = sheet.createRow(rownum);
                String author_id = rs.getString("author_id");
                String comment_author = rs.getString("comment_author");
                cell = row.createCell(1, CellType.STRING);
                author_id = author_id.substring(7);
                String[] res = author_id.split("}");
                author_id = res[0];
                cell.setCellValue(author_id);
                cell = row.createCell(2, CellType.STRING);
                cell.setCellValue(comment_author);

                rownum++;
            }
            sql = "with tt as (\n" +
                    "    select\n" +
                    "      comment_author,\n" +
                    "      comment_id,video_id\n" +
                    "    from comments\n" +
                    "),t as(\n" +
                    "    select\n" +
                    "      tt.comment_author,\n" +
                    "      count(tt.comment_id) as comments_count,\n" +
                    "      count(distinct tt.video_id) as video_count\n" +
                    "    from tt\n" +
                    "  group by tt.comment_author\n" +
                    ")\n" +
                    "select * from t\n" +
                    "order by comments_count desc";
            rs = stmt.executeQuery(sql);
            sheet = workbook.createSheet("Количество комментариев");
            rownum = 1;
            row = sheet.createRow(rownum);
            cell = row.createCell(1, CellType.STRING);
            cell.setCellValue("User");
            cell = row.createCell(2, CellType.STRING);
            cell.setCellValue("comments_count");
            cell = row.createCell(3, CellType.STRING);
            cell.setCellValue("video_count");
            rownum++;

            while (rs.next()) {
                row = sheet.createRow(rownum);
                String comment_author = rs.getString("comment_author");
                String comments_count = rs.getString("comments_count");
                String video_count = rs.getString("video_count");
                cell = row.createCell(1, CellType.STRING);
                cell.setCellValue(comment_author);
                cell = row.createCell(2, CellType.STRING);
                cell.setCellValue(comments_count);
                cell = row.createCell(3, CellType.STRING);
                cell.setCellValue(video_count);

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
        File file = new File("data_result.xls");
        //file.getParentFile().mkdirs();

        FileOutputStream outFile = new FileOutputStream(file);
        workbook.write(outFile);

    }

//выявление тегов из названия видео
    private static void test(String testString) {
        Pattern p = Pattern.compile("\\S+");
        Matcher m = p.matcher(testString); //выполняем проверка
        while (m.find()) {
            String tag = testString.substring(m.start(), m.end());
            tags_away.add(tag);
        }
    }
    public static String preparing(String comment){
       /* Pattern p = Pattern.compile("");
        Matcher m = p.matcher(comment);
        while(m.find()){

        }
        */

        String new_comment = comment.replace("&","&amp;");
        new_comment = new_comment.replace("'","&apos;").replace("<", "&lt;")
               .replace(">","&gt;");

       if(comment.compareTo(new_comment)>0){
       //    System.out.println("old:"+comment);
         //  System.out.println("new:"+new_comment);
       }
       return new_comment;
    }
    //метод, позволяющий корректно составлять граф
    private static void find(Map<String, Map<String, Map<String, String>>> nodes,String comment_author_id,
                             Map <String, Map<String,String>> help_data){
        if(nodes.containsKey(comment_author_id)){
            //старые данные
            Map<String,Map<String,String>> old_data=nodes.get(comment_author_id);
            //беребираем все старые данные и проверяем, нет ли еще данного пользователя в
            //ссылающихся
            for(Map.Entry<String, Map<String,String>> entry : help_data.entrySet()){
                if(old_data.containsKey(entry.getKey())){
                    continue;
                }
                //если нет, добавляем данные
                old_data.put(entry.getKey(),entry.getValue());
            }
            nodes.put(comment_author_id,old_data);
        }
        else {
            //заносим данные в  коллекцию вершин
            nodes.put(comment_author_id, help_data);
        }
    }

    private static boolean isPresent(String video_id) throws SQLException {
        boolean present = false;
       String sql = "select * from videos where video_id='"+video_id+"'";
        //String sql = "select comment_id from comments where video_id='"+video_id+"'";
        Statement stmt;
        stmt = c.createStatement(); //открываем соединение
        ResultSet rs = stmt.executeQuery(sql);
        if(rs.next())
            present = true;
        return present;
    }

    //потоки
    static class MyThread implements Runnable {
        double out;
        CommentThread currentComment;

       // MyThread(List<CommentThread> videoComments){
        //    this.
        //}
     /*   public void run(){

            System.out.printf("%s started... \n", Thread.currentThread().getName());

            try{

                Thread.sleep(500);
                String videoId = Thread.currentThread().getName();
                YouTube.Videos.List videosListByIdRequest = videosListByIdRequest = ChannelVideo.youtube.videos().list("id,statistics," +
                        "snippet,recordingDetails");
                videosListByIdRequest.setId(videoId);
                //API KEY
                videosListByIdRequest.setKey(Search.apiKey);
                //запрашиваем данные
                VideoListResponse listResponse = videosListByIdRequest.execute();
                Map<String, Map<String, String>> comments_data = new TreeMap<String, Map<String, String>>();

                Map<String, String> map = getInformation(listResponse.getItems().get(0));
                //берем видео только за 2018 год
                if(stop) {
                    System.out.println("stopped");
                    //    break;
                }
                //исключаем прямые эфиры
                if(map==null){
                    System.out.println("прямой эфир");
                }
                if (map != null){
                    test(map.get("title"));
                    //второй запрос - данные об авторе(канале) данного видео
                  //  YouTube.Channels.List channelsList= Search.youtube.channels().list("statistics,snippet,status,brandingSettings,localizations");
                    //channelsList.setId(map.get("channel_id"));
                    //channelsList.setKey(Search.apiKey);
                    //ChannelListResponse listResponse2 = channelsList.execute();
                    //getChannelInfo(listResponse2,map);

                    //является ли канал проверенным Confirmed
                    //out.writeStartElement("Confirmed");
                    // out.writeCharacters(map.get("confirmed"));
                    // out.writeEndElement();
                    //unixtime
                    Instant instant = Instant.parse(map.get("date"));
                    long millisecondsSinceUnixEpoch = instant.getMillis();

                    //автор данного видео
                    current_author_id = map.get("channel_id");
                    main_nodes_map = new HashMap<String, Map<String, String>>();

                    comments_count = map.get("comments_count");
                    all_comments_count += Integer.valueOf(comments_count);
                    if (!map.get("comments_count").equals("0")) {
                        // out.writeStartElement("Comments");
                        // out.writeEndElement();
                        String nextPage = "";
                        //перебираем все страницы = следующие запросы
                        do {
                            CommentThreadListResponse videoCommentsListResponse = ChannelVideo.youtube.commentThreads()
                                    .list("id,snippet").setVideoId(videoId).setTextFormat("plainText")
                                    .setMaxResults((long) 100).setPageToken(nextPage)
                                    .execute();
                            getVideoComments(videoCommentsListResponse, videoId, comments_data, comments, nodes_map, main_nodes_map);
                            nextPage = videoCommentsListResponse.getNextPageToken();
                        }
                        while (nextPage != null);
                        //заполняем карту данных для удобства
                        //ключ - тот, на кого ссылаются, значение - кто ссылается
                        //data.put(id, map);

                        comments.put(videoId, comments_data); //ДОБАВЛЯЕМ ВСЕ КОММЕНТАРИИ К ВИДЕО

                        db(map,videoId);
                    } else {

                    }
                  //  data.put(videoId, map);
                }
                //  }

                System.out.println(comments.size());

            }
            catch(InterruptedException e){
                System.out.println("Thread has been interrupted");
            } //catch (IOException e) {
            catch (XMLStreamException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            // e.printStackTrace();
            //} catch (XMLStreamException e) {
             //   e.printStackTrace();
           // }
            System.out.printf("%s finished... \n", Thread.currentThread().getName());
        }
        */

        public void run() {
            //     synchronized (comments){
            // do stuff with iterator e.g.
            //     Iterator<CommentThread> iterator = comments.iterator();
            //       while (iterator.hasNext()){
            //       currentComment = iterator.next();
            // }
            // }
            while (syncList.size() != 0) {

                synchronized (syncList) {
                    currentComment = getCurrentComment();
                }
                if(currentComment!=null) {
                    double in = System.currentTimeMillis();
                   // System.out.println(Thread.currentThread().getName() + " взял " + currentComment.getId() + " комментарий");
                    try {
                        get();
                        out = System.currentTimeMillis();
                        Thread.sleep(1000);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (XMLStreamException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }
                if (syncList.size() == 0) {
                    //  System.out.println("продолжаю");
                    cont = true;

            }

        }
        public synchronized void db(Map<String,String> map, String video_id) throws SQLException {
            Statement stmt;
            try {
                 stmt = c.createStatement();
                String sql = "INSERT INTO postgres.public.videos " +
                        "(video_id,video_title,author,publication_date,description,view_count," +
                        "likes_count,dislikes_count,comments_count,tags,channel_id,channel_follovers_count," +
                        "channel_video_count,channel_description, confirmation) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
                PreparedStatement preparedStatement = c.prepareStatement(sql);
                preparedStatement.setString(1, video_id);
                preparedStatement.setString(2, map.get("title"));
                preparedStatement.setString(3, map.get("author"));
                preparedStatement.setString(4, map.get("date"));
                preparedStatement.setString(5, map.get("description"));
                preparedStatement.setInt(6, Integer.valueOf(map.get("view_count")));
                preparedStatement.setInt(7, Integer.valueOf(map.get("likes_count")));
                preparedStatement.setInt(8, Integer.valueOf(map.get("dislikes_count")));
                preparedStatement.setInt(9, Integer.valueOf(map.get("comments_count")));
                preparedStatement.setString(10, map.get("tags"));
                preparedStatement.setString(11, map.get("channel_id"));
                preparedStatement.setInt(12, 0);
                preparedStatement.setInt(13, 0);
                preparedStatement.setString(14, map.get("channel_info"));
                preparedStatement.setBoolean(15, Boolean.valueOf(map.get("confirmed")));
                preparedStatement.execute();

                stmt.close();
                c.commit();
            }
            catch (SQLException e) {
                e.printStackTrace();
            }


        }

        public synchronized CommentThread getCurrentComment(){
            if(syncList.size()>0) {
                return syncList.remove(0);
            }
            else return null;
        }
        private void get() throws IOException, XMLStreamException {
            String id = currentComment.getId();
           // System.out.println("id="+id);
            Map<String, String> data = new HashMap<String, String>();
            List<String> id_of_repliers;
            CommentSnippet snippet = currentComment.getSnippet().getTopLevelComment()
                    .getSnippet();
            String data_id = snippet.getAuthorChannelId().toString().substring(7);
            String author_id = data_id.substring(0, data_id.length() - 1);
            //заполнение карты comments
            data.put("video_id", id);
            data.put("comment_id",currentComment.getId());
            data.put("author", snippet.getAuthorDisplayName());

            data.put("author_id", author_id);
            data.put("parent_id", current_author_id);
            //  System.out.println("comment_author:"+snippet.getAuthorDisplayName());

            data.put("comment", preparing(snippet.getTextDisplay()));
            data.put("date", snippet.getPublishedAt().toString());
            data.put("language", preparing(snippet.getTextOriginal()));
            //все главные комментарии
            //комментарий - данные
            comments_data.put(currentComment.getId(), data);

            //заполняем карту вершин

            // if(nodes.containsKey(current_author_id))
            data = new HashMap<String, String>();
            data.put("author_id", author_id);
            data.put("author_name", snippet.getAuthorDisplayName());
            data.put("parent_id", current_author_id);
            data.put("video_id", id);

            main_nodes_map.put(author_id, data); //author_id - адрес автора комментария

            get_replies(snippet, id, author_id, currentComment.getId(), nodes_map, comments_data);
          //  System.out.println(Thread.currentThread().getName()+" закончил работу");
       //     System.out.println( Thread.currentThread().getName()+" проработал "+(out-System.currentTimeMillis())/1000+" с.");

        }
    }
        public static void optimal_load_data(String user, String password, Map<String, Map<String, String>> from,
                                             boolean comments, String video_id) {
            Statement stmt;

            try {
                stmt = c.createStatement(); //открываем соединение
                //заполнение таблицы comments
                if (comments) {
                    boolean answer = false;
                    String sql = "INSERT INTO postgres.public.comments (comment_id, video_id, comment_text, comment_author, " +
                            "comment_date,author_id,parent_id,real_text,answer) VALUES (?,?,?,?,?,?,?,?,?)";
                    int i=0;
                    PreparedStatement stat = c.prepareStatement(sql);

                    for (Map.Entry<String, Map<String, String>> entry : from.entrySet()) {
                        stat.setString(1, entry.getValue().get("comment_id"));
                        stat.setString(2, video_id);
                        stat.setString(3, entry.getValue().get("comment"));
                        stat.setString(4, entry.getValue().get("author"));
                        stat.setString(5, entry.getValue().get("date"));
                        stat.setString(6, entry.getValue().get("author_id"));
                        stat.setString(7, entry.getValue().get("parent_id"));
                        stat.setString(8, entry.getValue().get("language"));
                        if(entry.getValue().get("comment_id").contains("."))
                            answer = true;
                        else
                            answer = false;
                        stat.setBoolean(9, answer);
                        stat.addBatch();
                        i++;
                        if ((i%1000) == 0 || i == from.entrySet().size()) {
                            stat.executeBatch(); // Execute every 1000 items.
                        }
                    }
                }
                //заполнение таблицы данных о видео
                else {
                    for (Map.Entry<String, Map<String, String>> entry : from.entrySet()) {

                        String sql = "INSERT INTO postgres.public.videos " +
                                "(video_id,video_title,author,publication_date,description,view_count," +
                                "likes_count,dislikes_count,comments_count,tags,channel_id,channel_follovers_count," +
                                "channel_video_count,channel_description, confirmation) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
                        PreparedStatement preparedStatement = c.prepareStatement(sql);
                        preparedStatement.setString(1, entry.getKey());
                        preparedStatement.setString(2, entry.getValue().get("title"));
                        preparedStatement.setString(3, entry.getValue().get("author"));
                        preparedStatement.setString(4, entry.getValue().get("date"));
                        preparedStatement.setString(5, entry.getValue().get("description"));
                        preparedStatement.setInt(6, Integer.valueOf(entry.getValue().get("view_count")));
                        preparedStatement.setInt(7, Integer.valueOf(entry.getValue().get("likes_count")));
                        preparedStatement.setInt(8, Integer.valueOf(entry.getValue().get("dislikes_count")));
                        preparedStatement.setInt(9, Integer.valueOf(entry.getValue().get("comments_count")));
                        preparedStatement.setString(10, entry.getValue().get("tags"));
                        preparedStatement.setString(11, entry.getValue().get("channel_id"));
                        preparedStatement.setInt(12, 0);
                        preparedStatement.setInt(13, 0);
                        preparedStatement.setString(14, entry.getValue().get("channel_info"));
                        preparedStatement.setBoolean(15, Boolean.valueOf(entry.getValue().get("confirmed")));
                       // i++;
                        preparedStatement.executeUpdate();
                        // System.out.println("-- Opened database successfully");
                    }
                }
                stmt.close();
                c.commit();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        }