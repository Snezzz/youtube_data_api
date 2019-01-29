## Сборка проекта
[![Build Status](https://travis-ci.org/Snezzz/youtube_data_api.svg?branch=master)](https://travis-ci.org/Snezzz/youtube_data_api)
[![Build Status](https://ci.appveyor.com/api/projects/status/github/Snezzz/youtube_data_api)](https://ci.appveyor.com/api/projects/status/github/Snezzz/youtube_data_api)  
Перед тем как собирать проект, убедитесь, что у вас стоит Java 1.8
# Проект по дипломной работе
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

