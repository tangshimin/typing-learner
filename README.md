## typing-learner

#### 一边打字一边记单词
https://user-images.githubusercontent.com/16540656/163662386-e82dc534-7a5a-4566-8449-fc71db51f960.mp4

#### 抄写字幕，可以抄写你感兴趣的电影、电视剧、纪录片、TED演讲。可以抄写多种语言的字幕。
[https://user-images.githubusercontent.com/16540656/166701653-130614b1-f586-44b7-9bb8-a0eb656160f9.mp4](https://user-images.githubusercontent.com/16540656/169644575-8f02e757-9105-430f-87e5-fe7bc93142e2.mp4)

demo 中的电影片段来源于 [Sintel](https://www.youtube.com/watch?v=eRsGyueVLvQ)。


### 主要功能：

1. [可以用MKV 视频生成词库(单词本)](https://github.com/tangshimin/typing-learner/wiki/%E4%BB%8E-MKV-%E8%A7%86%E9%A2%91%E7%94%9F%E6%88%90%E8%AF%8D%E5%BA%93)，让每个单词都有具体的语境。有了这个功能，今后就可以一边追美剧一边学英语了。
    ![Generate Vocabulary From MKV Video File](https://user-images.githubusercontent.com/16540656/166684580-57e31303-e849-4bb6-be9a-2cc0cb851317.png)
  
2. [如果不是 MKV 格式的视频可以使用字幕 + 视频生成词库(单词本)](https://github.com/tangshimin/typing-learner/wiki/%E4%BB%8E%E5%AD%97%E5%B9%95%E7%94%9F%E6%88%90%E8%AF%8D%E5%BA%93)
3. [可以用英文文档生成词库(单词本)](https://github.com/tangshimin/typing-learner/wiki/%E4%BB%8E%E6%96%87%E6%A1%A3%E7%94%9F%E6%88%90%E8%AF%8D%E5%BA%93)

4. 用 MKV 视频或字幕生成的词库，可以链接到用文档生成的词库或内置的词库。下面着张图片表示，电影 Sintel 的所有字幕中，有 9 条字幕，匹配了四级词库中的 6 个单词。
   
   ![Link Vocabulary](https://user-images.githubusercontent.com/16540656/166690274-2075b736-af51-42f0-a881-6535ca11d4d3.png)
  
5. 过滤词库，过滤熟悉的单词。

6. 抄写字幕，可以抄写你感兴趣的电影、电视剧、纪录片、TED演讲。可以抄写多种语言的字幕。

7. 歌词转字幕

8. 抄写文本，可以抄写 [古腾堡计划](https://www.gutenberg.org/) 所有 txt 格式的电子书，非[古腾堡计划](https://www.gutenberg.org/) 的电子书，抄写前可能需要先格式化，把每行的字母数量限制在 75 个以内。

9. 合并词库，可以把一整季的电视剧生成的多个字幕词库合并成一个词库。

10. 学习完一章之后，可以选择进入默写模式，在默写模式整个章节的单词是重新随机排序的，默写完了会出现默写的正确率。
  
11. 学习完整个词库之后，还有一个随机排序整个词库的功能。
  
12.  默认使用 Enter 键切换下一个单词，如果要使用 [qwerty-learner](https://github.com/Kaiyiwing/qwerty-learner) 的自动切换，可以使用 Ctrl + A 开启自动切换。推荐第一次记忆不认识的单词时，使用非自动切换，第二天复习的时候再打开自动切换。 

## [下载地址](https://github.com/tangshimin/typing-learner/releases)

## TODO
- [ ] 1.1  生成词库的时候可以使用本地的`mdx`格式的词典。

## 开发环境

- 启动项目之前需要将 `typing-learner\resources\common\dictionary` 文件夹里的词典文件`ecdict.mv.db.7z` 解压缩。不然不能使用生成词库功能。
- JDK 17
- gradle 7.3.3
- VLC 视频播放器


## 致谢
本项目的核心功能，记忆单词来源于  [qwerty-learner](https://github.com/Kaiyiwing/qwerty-learner) ，感谢 qwerty-learner 的所有贡献者，让我有机会把我曾经放弃的一个 app，又找到新的方式实现。

感谢 [skywind3000](https://github.com/skywind3000) 开源 [ECDICT](https://github.com/skywind3000/ECDICT)。

感谢 [libregd](https://github.com/libregd) 为本项目设计 Logo。


