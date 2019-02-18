import org.codehaus.jackson.map.ObjectMapper;
import org.gephi.preview.api.G2DTarget;
import org.gephi.project.api.ProjectController;
import org.openide.util.Lookup;

import javax.swing.*;
import javax.xml.stream.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.sql.*;
import java.util.*;

public class GetData {
    public static Connection c;
    static Statement stmt,stmt2;
    static String sql;
    private static XMLStreamWriter out;
    private static XMLStreamReader in;
    public static  Map <String,Map <String, String>> nodes;
    public static Map <String,Integer> comments_count;
    public static ProjectController pc;

    public static void run() throws SQLException, IOException, XMLStreamException {
        comments_count=new HashMap<String, Integer>();
        //соединение с БД
        connect();
        //получение вершин (запрос 1)
        getNodes();
        getObject();

        //построение графа
        pc = Lookup.getDefault().lookup(ProjectController.class);
        /*try {
            G2DTarget target = null;
            new GraphCreater(1,"",true,false,false,false,target);//создание графа
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        */
        //3.визуализация
        pc = Lookup.getDefault().lookup(ProjectController.class);
        JFrame menu=new JFrame("SetData");
        //menu.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        final JPanel panel=new JPanel();
        final JTextField count=new JTextField(2);
        count.setText("1");
        String[] items = {
                "Default",
                "Betweeness_centrality",
                "Page_rank",
                "Modularity"
        };
        final JComboBox box=new JComboBox(items);
        box.setEditable(false);
        final JButton button=new JButton("Построить");

        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean Default = false;
                boolean betweeness = false;
                boolean page_rank=false;
                boolean modularity=false;
                int k=Integer.valueOf(count.getText());
                String type=box.getSelectedItem().toString();
                if(type.equals("Betweeness_centrality"))
                    betweeness =true;
                else if(type.equals("Page_rank"))
                    page_rank=true;
                else  if(type.equals("Modularity"))
                    modularity=true;
                else{
                    Default = true;
                }
                try {
                    G2DTarget target = null;
                    new GraphCreater(k,"",Default,betweeness,page_rank,modularity,target);//создание графа
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

            }
        });

        panel.add(count);
        panel.add(box);
        panel.add(button);
        menu.add(panel);

        menu.setSize(200,200);
        menu.setLocation(400,150);

        menu.setVisible(true);


/*
//анализ данных
        Results results = new Results();
        //1.количество просмотров/количество комментариев/количество собеседников
        String sql = "with t1 as(select distinct videos.video_id, comment_author from videos\n" +
                "  join comments on videos.video_id=comments.video_id),\n" +
                "  t2 as (select video_id,video_title,view_count,comments_count from videos)\n" +
                "  select t1.video_id,count(comment_author) as people_count,t2.view_count,t2.comments_count from t1 join t2 on t1.video_id=t2.video_id\n" +
                "  group by t1.video_id,t2.view_count,t2.comments_count;";
        //results.get(sql);
        //2.id пользователя/количество комментариев/количество прокомментированных видео/количество каналов
       results.get(sql,"1");
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
               "    group by tt.comment_author\n" +
               "), t2 as (select distinct tt.video_id,tt.comment_author,author from tt join videos on tt.video_id=videos.video_id)\n" +
               "--select * from t2\n" +
               "  select distinct t2.comment_author,count(distinct author) as channels_count,t.comments_count,t.video_count\n" +
               "    as commented_video_count from t2\n" +
               "    join t on t2.comment_author=t.comment_author group by t2.comment_author,t.comments_count,t.video_count order by comments_count desc";
             results.get(sql,"2");
             //фильтр по количеству разных каналов (авторов)
             int channels_count = 1;
             sql = "with tt as (\n" +
                     "    select\n" +
                     "      comment_author,\n" +
                     "      author_id,\n" +
                     "      comment_id,video_id\n" +
                     "    from comments\n" +
                     "),t as(\n" +
                     "    select\n" +
                     "      tt.comment_author,\n" +
                     "      count(tt.comment_id) as comments_count,\n" +
                     "      count(distinct tt.video_id) as video_count\n" +
                     "    from tt\n" +
                     "    group by tt.comment_author\n" +
                     "), t2 as (select distinct tt.video_id,tt.comment_author,tt.author_id,author from tt join videos on tt.video_id=videos.video_id)\n" +
                     "--select * from t2\n" +
                     "  , t3 as (select distinct t2.comment_author,t2.author_id,count(distinct author) as channels_count,t.comments_count,t.video_count\n" +
                     "    as commented_video_count from t2\n" +
                     "    join t on t2.comment_author=t.comment_author\n" +
                     "  group by t2.comment_author,t2.author_id,t.comments_count,t.video_count order by comments_count desc)\n" +
                     "  select * from t3 where channels_count >= " + channels_count;
             results.get(sql,"Фильтр по количеству каналов = " + channels_count);

            //количество прокомментированных видео и количество разных каналов
             sql = "select comment_author,count(videos.video_id) as video_count , videos.channel_id, videos.author as channel from comments\n" +
                     "  join videos on comments.video_id = videos.video_id  group by comment_author, videos.channel_id,videos.author order by comment_author";
             results.get(sql, "видео и каналы");
             sql = "with t1 as(select distinct author_id,comment_author,videos.video_id, channel_id from comments join videos\n" +
                     "    on comments.video_id = videos.video_id),\n" +
                     "  t2 as (select distinct author_id,comment_author,videos.video_id, channel_id from comments join videos\n" +
                     "      on comments.video_id = videos.video_id)\n" +
                     "select t2.comment_author, t2.author_id,count(t1.comment_author) as channels_count from t1 join t2 on t1.comment_author=t2.comment_author\n" +
                     "where t1.channel_id!=t2.channel_id group by t2.comment_author,t2.author_id";
             results.get(sql, "5");
             sql= "with t1 as(select distinct author_id,comment_author,videos.video_id, channel_id from comments join videos\n" +
                     "    on comments.video_id = videos.video_id),\n" +
                     "  t2 as (select distinct author_id,comment_author,videos.video_id, channel_id from comments join videos\n" +
                     "      on comments.video_id = videos.video_id), t3 as(\n" +
                     "select t2.comment_author, t2.author_id,count(t1.comment_author) as channels_count from t1 join t2 on t1.comment_author=t2.comment_author\n" +
                     "where t1.channel_id!=t2.channel_id group by t2.comment_author,t2.author_id),\n" +
                     "  t4 as (select count(distinct comment_author) as user_count from comments)\n" +
                     "  select count(t3.comment_author) as count, t4.user_count from t3, t4 group by user_count";
             //6
             results.get(sql,"6");
             results.put(); //создание .xls
*/
    }

    private static void connect() throws SQLException {
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
        stmt = c.createStatement(); //открываем соединение
    }
    private static void getNodes() throws SQLException {

        sql="select parent_id,channel_id,author_id,count(parent_id) from comments\n" +
                "  join videos on comments.video_id = videos.video_id group by parent_id,channel_id,author_id";
        ResultSet rs = stmt.executeQuery(sql);
        String parend_id,author_id, count;
        nodes = new HashMap<String, Map<String, String>>();

        //обращаемся к каждой строке
        while(rs.next()){
            parend_id = rs.getString(1);
            author_id = rs.getString(3);
            count = rs.getString(4);
            if(nodes.containsKey(parend_id)){
                Map <String, String> old_map = new HashMap<String, String>();
                old_map = nodes.get(parend_id);
                old_map.put(author_id,count);
                nodes.put(parend_id,old_map);
            }
            else{
                Map<String, String> children = new HashMap<String, String>();
                children.put(author_id,count);
                nodes.put(parend_id,children);
            }
        }

    }
    private static void getObject() throws SQLException, IOException, XMLStreamException {
        List<String> video = new LinkedList<String>();
        String sql = "select video_id from videos";
        ResultSet rs = stmt.executeQuery(sql);
        while(rs.next()){
            video.add(rs.getString(1));
        }
        Map <String, Object> main_map = new LinkedHashMap<String, Object>();
        //для каждого видео получаем комментарии
        Iterator it = video.iterator();
        while (it.hasNext()) {
            String video_id = it.next().toString();
            get_comments_count(video_id);
            Map <String, Object> info = new LinkedHashMap<String, Object>();
            sql = "select * from videos where video_id='"+video_id+"'";
            rs = stmt.executeQuery(sql);
            Map <String, Object> main_details = new LinkedHashMap<String,Object>();
            //основная часть
            while(rs.next()) {
                info.put("ID", video_id);
                info.put("Header", rs.getString("video_title"));
                info.put("Body", rs.getString("description"));
                info.put("Username", rs.getString("author"));
                info.put("UserID", rs.getString("channel_id"));
                info.put("Confirmed", "");
                info.put("Date", rs.getString("publication_date"));
                get_tags(rs.getString("tags"),info);
                main_details.put("like",rs.getString("likes_count"));
                main_details.put("dislike",rs.getString("dislikes_count"));
                main_details.put("Views",rs.getString("view_count"));
            }
            //получили все комментарии для данного видео
            sql = "select * from comments where video_id = '" + video_id+"' and answer=false";
            rs = stmt.executeQuery(sql);
            stmt2 = c.createStatement(); //открываем соединение


            Map<String, Map<String,Object>> comments = new LinkedHashMap<String, Map<String, Object>>();
            Map<String,Object> data;
            //перебираем каждый комментарий
            int i=0;
            while(rs.next()) {

                //System.out.println("comment"+i+":"+rs.getString("real_text"));
                i++;
                data = new LinkedHashMap<String, Object>();
                String username = rs.getString("comment_author");
                String userID = rs.getString("author_id");
                String text = rs.getString("real_text");
                data.put("Username",username);
                data.put("UserID",userID);
                data.put("Text",text);
                sql = "select * from comments where video_id = '" + video_id + "' " +
                        "and parent_id='" + rs.getString("author_id") + "'";
                ResultSet resultSet = stmt2.executeQuery(sql);

               //получаем ответы
                Map <String, Map <String,String>> answers = null;
                Map <String,String> details = null;
                answers = new LinkedHashMap<String, Map<String, String>>();
                    while (resultSet.next()) {
                        if(!resultSet.getString("author_id").equals("")) {
                            details = new LinkedHashMap<String, String>();
                        //кладем данные в details
                        String Username = resultSet.getString("comment_author");
                        String UserID = resultSet.getString("author_id");
                        String Text = resultSet.getString("real_text");
                        details.put("Username", Username);
                        details.put("UserID", UserID);
                        details.put("Text", Text);
                        //
                        String comment_id = resultSet.getString("comment_id"); //?
                        //информация о комментарии
                        answers.put(comment_id, details);
                      //  System.out.println(resultSet.getString("author_id"));
                    }


                }
                //добавляем ответы
                data.put("Comments",answers);
                comments.put(rs.getString("comment_id"),data);

            }

            main_details.put("Comments",comments);
            info.put("Details",main_details);
            //для каждого видео вносим данные
            main_map.put(video_id,info);
        }
            create_xml(main_map);
            //check_xml();
    }
    private static void check_xml() throws IOException, XMLStreamException {
        File file = new File("results.xml");
        InputStream inputStream = new FileInputStream(file);
        in = XMLInputFactory.newInstance().createXMLStreamReader(new InputStreamReader(inputStream,"utf-8"));
        String text = in.getText();
        String result = SetData.preparing(text);
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write(result);
        fileWriter.close();

    }
    private static void create_xml( Map <String, Object> map)throws IOException, XMLStreamException {
        OutputStream outputStream = new FileOutputStream(new File("results.xml"));
        out = XMLOutputFactory.newInstance().createXMLStreamWriter(
                new OutputStreamWriter(outputStream, "utf-8"));
        out.writeStartDocument();
        out.writeStartElement("Root");
        for(Map.Entry<String,Object> entry : map.entrySet()) {
            out.writeStartElement("Item");
            ObjectMapper oMapper = new ObjectMapper();
            Map<String, Object> value = oMapper.convertValue(entry.getValue(), Map.class);

            for(Map.Entry<String,Object> entry2 : value.entrySet()) {
                if(entry2.getKey().equals("Tags")){
                    out.writeStartElement(entry2.getKey());
                   ArrayList res = oMapper.convertValue(entry2.getValue(), ArrayList.class);
                   Iterator it = res.iterator();
                   while(it.hasNext()){
                       out.writeStartElement("Tag");
                       out.writeCharacters(it.next().toString());
                       out.writeEndElement();
                   }
                   out.writeEndElement();
                }
                else if (!entry2.getKey().equals("Details")) {
                    out.writeStartElement(entry2.getKey());
                    out.writeCharacters(entry2.getValue().toString());
                    out.writeEndElement();
                } else {
                    Map<String, Object> value2 = oMapper.convertValue(entry2.getValue(), Map.class);
                    out.writeStartElement(entry2.getKey()); //details
                    for (Map.Entry<String, Object> entry4 : value2.entrySet()) {
                        if (entry4.getKey().equals("like")) {
                            out.writeStartElement("Emotions");
                            out.writeEmptyElement("Emotion");
                            out.writeAttribute("type", "like");
                            out.writeAttribute("count", entry4.getValue().toString());
                           // out.writeEndElement();
                        }
                        else if (entry4.getKey().equals("dislike")) {
                            out.writeEmptyElement("Emotion");
                            out.writeAttribute("type", "dislike");
                            out.writeAttribute("count", entry4.getValue().toString());
                            //out.writeEndElement();
                        }
                        else if (entry4.getKey().equals("Views")) {
                            out.writeEndElement();
                            out.writeEmptyElement("Views");
                            out.writeAttribute("count", entry4.getValue().toString());
                           //out.writeEndElement();
                        } else {
                            out.writeStartElement("Comments");
                            Map<String, Object> comments = oMapper.convertValue(entry4.getValue(), Map.class);
                            out.writeAttribute("count", String.valueOf(comments.size()));
                            for (Map.Entry<String, Object> entry3 : comments.entrySet()) {
                                out.writeStartElement("Comment");
                                Map<String, Object> data = oMapper.convertValue(entry3.getValue(), Map.class);
                                for (Map.Entry<String, Object> entry5 : data.entrySet()) {
                                    if((entry5.getKey().equals("Text"))&&(entry5.getValue().toString()
                                            .equals("\uD83D\uDD1D\uD83D\uDD1D\uD83D\uDD1D"))){
                                        System.out.println(String.valueOf(entry5.getValue()));
                                    }
                                    if (!entry5.getKey().equals("Comments")) {
                                        out.writeStartElement(entry5.getKey());

                                        out.writeCharacters(String.valueOf(entry5.getValue()));
                                        out.writeEndElement();
                                    }
                                    //ответы есть
                                    else {
                                        Map<String, Object> answers = oMapper.convertValue(entry5.getValue(), Map.class);
                                        if (answers.size() > 0) {
                                            out.writeStartElement("Comments");
                                            out.writeAttribute("count", String.valueOf(answers.size()));
                                            put(oMapper, answers, "Comment");
                                        }
                                    }
//
                                }
                                out.writeEndElement(); //comment
                            }
                            out.writeEndElement(); //comments

                        }
                    }
                    out.writeEndElement(); //details
                }
            }
            System.out.println("");
            out.writeEndElement(); //item
        }
        out.writeEndElement();
        out.writeEndDocument();
        out.close();

    }
    private static void get_comments_count(String videoID) throws SQLException {
        Statement stmt;
        ResultSet resultSet=null;
        try {
            System.out.println("-- Opened database successfully");
            String sql;
            stmt = c.createStatement(); //открываем соединение
            sql="select comment_author,author_id,parent_id,count(parent_id)" +
                    " from comments where video_id='"+videoID+"'" +
                    " group by comment_author,author_id,parent_id";
            resultSet=stmt.executeQuery(sql);
            while (resultSet.next()){
                String who_to_whom=resultSet.getString("author_id")+"!"
                        +resultSet.getString("parent_id");
                comments_count.put(who_to_whom,
                        Integer.valueOf(resultSet.getString("count")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
        private static void put(ObjectMapper oMapper,Map<String,Object> map,String name) throws XMLStreamException {
            for(Map.Entry<String,Object> entry : map.entrySet()) {
                out.writeStartElement(name);
                Map<String, Object> value = oMapper.convertValue(entry.getValue(), Map.class);
                for (Map.Entry<String, Object> entry2 : value.entrySet()) {
                        out.writeStartElement(entry2.getKey());
                        out.writeCharacters(String.valueOf(entry2.getValue()));
                        out.writeEndElement();
                    }
                out.writeEndElement(); //comment
                }
                out.writeEndElement(); //comment

        }
        private static void get_tags(String from,Map <String, Object> to){
            String [] tags = from.substring(1,from.length()-1).split(",");
            to.put("Tags",tags);
        }
}
