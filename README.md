# Проект по дипломной работе
## Сборка проекта
[![Build Status](https://travis-ci.org/Snezzz/youtube_data_api.svg?branch=master)](https://travis-ci.org/Snezzz/youtube_data_api)
[![Build Status](https://ci.appveyor.com/api/projects/status/github/Snezzz/youtube_data_api)](https://ci.appveyor.com/api/projects/status/github/Snezzz/youtube_data_api)  

### Системные требования:
  * OS: Windows x86/x86_64, Linux x86/x86_64  
  * Оперативная память (RAM): 13n Free RAM, где n - минимальное количество видео в запросе  
  * Для сборки: версия Java не ниже 1.8 
### Сборка из исходников:  
1.Установите Java  
2.Установите Apache Maven (http://maven.apache.org)  
3.В терминале: ```mvn compile```  
4.```mvn assembly:single```  

## Запуск проекта:  
### Вариант 1:  
[Скачайте](https://drive.google.com/file/d/1Dliqm6TVPEMTNP4pFhRUwglm-fZi3lEa/view?usp=sharing) готовый билд
### Вариант 2:  
в консоли: ```java -jar target/youtube_data-0.0.1-jar-with-dependencies.jar```

## Описание проекта
  Проект посвящен анализу AD HOC дискусский в социальной сети Youtube  
    Язык программирования: Java   
    Используемые библиотеки: Gson, YouTube Data API, Gephi Toolkit, SaX,XmlPull  
    База данных: Postgres
  
  На данный момент реализован сбор данных по каждому видео в ютуб по введенному запросу, построение и отображение графа, где вершины - пользователи, ребра - наличие взаимодействия между пользователями(по комментариям)
  ## Визуализация (k=миним степень вершин,v=количество видео)
  ### PageRank, k=2, v=18
  ![Screenshot](results/результат_1.png)
  ### Betweeness centrality, k=2, v=18
   ![Screenshot](results/результат_2.png)
  ### Modularity, k=3, v=19
   ![Screenshot](results/результат_3.png)
  ### Betweeness centrality, k=1, v=2
   ![Screenshot](results/результат_4.png)
   ### Modularity, k=1, v=2
   ![Screenshot](results/результат_5.png)

