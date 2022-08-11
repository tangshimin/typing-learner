## Typing Learner
  可以用 MKV 视频、字幕或文档生成词库（单词本），让每个单词都有具体的语境。然后通过语境记忆单词，句子，字幕。

## 主要功能：

1. [可以用MKV 视频生成词库(单词本)](https://github.com/tangshimin/typing-learner/wiki/%E4%BB%8E-MKV-%E8%A7%86%E9%A2%91%E7%94%9F%E6%88%90%E8%AF%8D%E5%BA%93)，让每个单词都有具体的语境。

    ![Generate Vocabulary From MKV Video File](https://user-images.githubusercontent.com/16540656/166684580-57e31303-e849-4bb6-be9a-2cc0cb851317.png)
    
2. 记忆单词，记忆单词的时候，会自动播放单词的读音，然后用键盘打字练习拼写，每个单词都可以输入多次，直到记住为止。从 MKV 生成的词库(单词本)，可以播放单词对应的视频片段。每个单元有 20 个单词，记完一个单元还有听写测试，检查记忆效果。默认使用 Enter 键切换下一个单词。
    

    https://user-images.githubusercontent.com/16540656/163662386-e82dc534-7a5a-4566-8449-fc71db51f960.mp4
    
    demo 中的电影片段来源于 [Sintel](https://www.youtube.com/watch?v=eRsGyueVLvQ)。
    
3. 听写复习，可以选择多个章节的单词一起复习，先听写测试，然后再复习错误的单词。

    ![DictionReview](https://user-images.githubusercontent.com/16540656/184179317-f8c0ac99-9048-48da-b59b-5badbaae7c62.png)

4. 抄写字幕，可以抄写你感兴趣的电影、电视剧、纪录片、TED演讲、歌词。可以抄写多种语言的字幕。
5. 抄写字幕界面也可用来练习听力，可用重复的播放一句字幕，直到听懂为止。切换到下一条字幕用 `Enter` 或 `↓`键。

    https://user-images.githubusercontent.com/16540656/174944474-e5947df9-c8ed-4546-9c67-057fe52c2d51.mp4
    
6. 抄写文本，可以抄写 [古腾堡计划](https://www.gutenberg.org/) 所有 txt 格式的电子书，非[古腾堡计划](https://www.gutenberg.org/) 的电子书，抄写前可能需要先格式化，把每行的字母数量限制在 75 个以内。

   ![Demo-Text](https://user-images.githubusercontent.com/16540656/175084580-6b26abc3-671f-455e-ac5f-aa583297a0e0.png)
   

7. [不是 MKV 格式的视频可以使用字幕 + 视频生成词库(单词本)](https://github.com/tangshimin/typing-learner/wiki/%E4%BB%8E%E5%AD%97%E5%B9%95%E7%94%9F%E6%88%90%E8%AF%8D%E5%BA%93)

8. [可以用英文文档生成词库(单词本)](https://github.com/tangshimin/typing-learner/wiki/%E4%BB%8E%E6%96%87%E6%A1%A3%E7%94%9F%E6%88%90%E8%AF%8D%E5%BA%93)遇见一遍生成比较多的英文文档，有不想一遍查词典一遍看文档，先用文档生成词库，把生成先记一遍，然后看文档的时候会更加流畅。

9. 用 MKV 视频或字幕生成的词库，可以链接到用文档生成的词库或内置的词库。下面着张图片表示，电影 Sintel 的所有字幕中，有 9 条字幕，匹配了四级词库中的 6 个单词。
   
   ![Link Vocabulary](https://user-images.githubusercontent.com/16540656/166690274-2075b736-af51-42f0-a881-6535ca11d4d3.png)
  
10. 过滤词库，过滤熟悉的单词。

11. 歌词转字幕 

12. 合并词库，可以把一整季的电视剧生成的多个字幕词库合并成一个词库。

13. 内置了常用词库(单词本)：四级、六级、专四、专八、考研、TOEFL、IELTS、GRE、GMAT、SAT、牛津核心词、北师大版高中英语、人教版英语、商务英语、外研版英语、新概念英语。这些词是没有链接字幕的，后续可用根据自己的兴趣，链接字幕词库。

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
- JDK 17
- gradle 7.3.3
- VLC 视频播放器


## 致谢
本项目的记忆单词功能来源于  [qwerty-learner](https://github.com/Kaiyiwing/qwerty-learner) ，感谢 qwerty-learner 的所有贡献者，让我有机会把我曾经放弃的一个 app，又找到新的方式实现。

感谢 [skywind3000](https://github.com/skywind3000) 开源 [ECDICT](https://github.com/skywind3000/ECDICT)。

感谢 [libregd](https://github.com/libregd) 为本项目设计 Logo 和主页，为本项目贡献了非常好的 Feature,和一些交互设计。



