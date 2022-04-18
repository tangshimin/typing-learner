# qwerty-learner-desktop

感谢 [qwerty-learner](https://github.com/Kaiyiwing/qwerty-learner) 的所有贡献者，让我有机会把我曾经放弃的一个 app，又找到新的方式实现。

感谢 [skywind3000](https://github.com/skywind3000) 开源 [ECDICT](https://github.com/skywind3000/ECDICT)， qwerty leaner desktop 的所有的本地词典数据都来源于 ECDICT。现在程序使用的是一个基础版有76万词条，完整版的数据实在太大，导入到 H2 数据库之后有500M,后续可能会增加一个功能，加载那个350万词条的数据库。

https://user-images.githubusercontent.com/16540656/163662386-e82dc534-7a5a-4566-8449-fc71db51f960.mp4

### 主要新增功能如下：

1. 可以用[电影](https://github.com/tangshimin/qwerty-learner-desktop/wiki/%E4%BB%8E-MKV-%E8%A7%86%E9%A2%91%E7%94%9F%E6%88%90%E8%AF%8D%E5%BA%93)或[字幕](https://github.com/tangshimin/qwerty-learner-desktop/wiki/%E4%BB%8E%E6%96%87%E6%A1%A3%E7%94%9F%E6%88%90%E8%AF%8D%E5%BA%93)生成词库，让每个单词都有具体的语境。
  
2. 可以[用英文文档生成词库](https://github.com/tangshimin/qwerty-learner-desktop/wiki/%E4%BB%8E%E6%96%87%E6%A1%A3%E7%94%9F%E6%88%90%E8%AF%8D%E5%BA%93)，比如你需要学习 TypeScript 最新的文档又没有中文版，你就可以用英文文档生成词库，然后过滤掉一些你熟悉的词，剩下的就是一些陌生的单词。把陌生单词学习几遍之后再阅读技术文档就更流畅了。
  
3. 基于文档的词库可以链接用 MKV 视频或字幕生成的词库。
  
4. 可以删除和修改词库。
  
5. 音频资源会自动缓存。
  
6. 学习完一章之后，可以选择进入默写模式，在默写模式整个章节的单词是重新随机排序的，默写完了会出现默写结果。
  
7. 学习完整个词库之后，还有一个随机排序整个词库的功能。
  
8. 单词可以重复输入多次。
  
9. 字幕和单词英语定义也可以重复输入多次。
  
10. 增加了词性，词形和英语定义等属性，可以自由的关闭和隐藏。
  
11. 可以扩展其他语言，只要有类似于 [ECDICT](https://github.com/skywind3000/ECDICT) 的本地词典

12. 默认使用 Enter 键切换下一个单词，如果要使用类似于网页版的自动切换，可以使用 Ctrl + A 开启自动切换。


### 大文件拉取
本地词库是一个大文件(265MB)如果安装了[Git Large File Storage (LFS)](https://git-lfs.github.com/) 可以直接 `clone`即可。若在安装 Git LFS 之前执行了 `clone`，则拉取下来的仓库并不包含大文件本体，而是一个指向其 LFS 存储对象的文件指针，这种情况需要使用 `git lfs pull` 拉取文件指针所指向的完整对象。



### 开发环境
- jdk 17
- VLC 视频播放器
