## Typing Learner 2.0 已经更名为幕境并迁移到了新的仓库。请前往[幕境仓库](https://github.com/tangshimin/MuJing)以获取最新版本和更新

## 主要功能：

1. [用 MKV 的电影、电视剧生成词库(单词本)](https://github.com/tangshimin/typing-learner/wiki/%E4%BB%8E-MKV-%E8%A7%86%E9%A2%91%E7%94%9F%E6%88%90%E8%AF%8D%E5%BA%93)，让每个单词都有具体的语境。

    ![Demo-Generate-Vocabulary-Light](https://user-images.githubusercontent.com/16540656/184311741-15fab9c3-83ba-4080-bac7-ca3a163c67d0.png)

2. [不是 MKV 格式的视频可以使用字幕 + 视频生成词库(单词本)](https://github.com/tangshimin/typing-learner/wiki/%E4%BB%8E%E5%AD%97%E5%B9%95%E7%94%9F%E6%88%90%E8%AF%8D%E5%BA%93)

    
3. 记忆单词，记忆单词的时候，会自动播放单词的读音，然后用键盘打字练习拼写，每个单词都可以输入多次，直到记住为止。从 MKV 生成的词库(单词本)，可以抄写单词对应的字幕，播放单词对应的视频片段。每个单元有 20 个单词，记完一个单元还有听写测试，检查记忆效果。默认使用 Enter 键切换下一个单词。
    

    https://user-images.githubusercontent.com/16540656/163662386-e82dc534-7a5a-4566-8449-fc71db51f960.mp4
    
    demo 中的电影片段来源于 [Sintel](https://www.youtube.com/watch?v=eRsGyueVLvQ)。
    
4. 听写复习，可以选择多个章节的单词一起复习，先听写测试，然后再复习错误的单词。

    ![DictionReview](https://user-images.githubusercontent.com/16540656/184179317-f8c0ac99-9048-48da-b59b-5badbaae7c62.png)

5. 抄写字幕，可以抄写你感兴趣的电影、电视剧、纪录片、TED演讲、歌词。可以抄写多种语言的字幕。
6. 抄写字幕界面也可用来练习听力，可用重复的播放一句字幕，直到听懂为止。<br>
   也可以用来练习口语，可以重复的跟读一条字幕，直到流畅为止。 <br>
   切换到下一条字幕用 `Enter` 或 `↓`键。

    https://user-images.githubusercontent.com/16540656/174944474-e5947df9-c8ed-4546-9c67-057fe52c2d51.mp4
    
7. 抄写文本，可以抄写 [古腾堡计划](https://www.gutenberg.org/) 所有 txt 格式的电子书，非[古腾堡计划](https://www.gutenberg.org/) 的电子书，抄写前可能需要先格式化，把每行的字母数量限制在 75 个以内。

   ![Demo-Text](https://user-images.githubusercontent.com/16540656/175084580-6b26abc3-671f-455e-ac5f-aa583297a0e0.png)


8. [用英文文档生成词库(单词本)](https://github.com/tangshimin/typing-learner/wiki/%E4%BB%8E%E6%96%87%E6%A1%A3%E7%94%9F%E6%88%90%E8%AF%8D%E5%BA%93)要读一篇陌生单词比较多的英文文档，又不想一边查词典一边看文档，可以先用文档生成词库，把陌生单词先记一遍，然后看文档的时候会更加流畅。

9. [用 MKV 视频或字幕生成的词库，可以链接到用文档生成的词库或内置的词库](https://github.com/tangshimin/typing-learner/wiki/链接字幕词库)。下面着张图片表示，电影 Sintel 的所有字幕中，有 9 条字幕，匹配了四级词库中的 6 个单词。
   
   ![Link Vocabulary](https://user-images.githubusercontent.com/16540656/166690274-2075b736-af51-42f0-a881-6535ca11d4d3.png)
  
10. 过滤词库，过滤熟悉的单词。

11. 歌词转字幕 

12. 合并词库，可以把一整季的电视剧生成的多个字幕词库合并成一个词库。

13. 内置了常用词库(单词本)：四级、六级、专四、专八、考研、TOEFL、IELTS、GRE、GMAT、SAT、牛津核心词、北师大版高中英语、人教版英语、商务英语、外研版英语、新概念英语。这些词是没有链接字幕的，后续可用根据自己的兴趣，[链接字幕词库](https://github.com/tangshimin/typing-learner/wiki/链接字幕词库)。

14. 内置了一个包含 770612 个词条的本地词典，这个词典来自于 [ECDICT](https://github.com/skywind3000/ECDICT) 的基础版，可以用快捷键 `Ctrl + F` 打开搜索框，查询单词。

   

## 应用平台：Windows / macOS   
### [下载地址](https://github.com/tangshimin/typing-learner/releases)

## 数据来源
#### 内置词库
内置词库的单词数据来源于 [qwerty-learner](https://github.com/Kaiyiwing/qwerty-learner) 的词库数据, 然后用 [ECDICT](https://github.com/skywind3000/ECDICT) 做了一些处理，原始词库只保留了【英语单词】和【音标】。
#### 本地词典
生成词库所使用的本地词典数据来源于 [ECDICT](https://github.com/skywind3000/ECDICT)
#### 发音数据
单词的发音数据来源于 [有道词典](https://www.youdao.com/) 的在线发音 API


## 开发环境

- 启动项目之前需要将 `typing-learner\resources\common\dictionary` 文件夹里的词典文件`ecdict.mv.db.7z` 解压缩。不然不能使用生成词库功能。打包之前要把`ecdict.mv.db.7z`删掉。
- Windows : JDK 17.0.1(Windows 11 JDK 17.0.1 以后的版本，打开对话框后，标题栏会消失 Compose Desktop Issue 2488，所以就把 JDK 改成了 JDK 17.0.1) 
- macOS : JDK 17.0.5(JDK 17 最新版)
- gradle 7.3.3
- VLC 视频播放器


## 致谢
感谢 [qwerty-learner](https://github.com/Kaiyiwing/qwerty-learner)  的所有贡献者，让我有机会把我曾经放弃的一个 app，又找到新的方式实现。

感谢 [skywind3000](https://github.com/skywind3000) 开源 [ECDICT](https://github.com/skywind3000/ECDICT)。

感谢 [libregd](https://github.com/libregd) 为本项目设计 Logo,和一些交互设计，以及非常好的功能建议。




