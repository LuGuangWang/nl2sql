package com.wlg.nl2sql.tools;

import com.wlg.nl2sql.beans.MetadataBean;
import com.wlg.nl2sql.datas.Datas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * 分词
 */
public class WordTool {
    private final List<String> english_word = new ArrayList<>();
    private final String regex = "[A-Z|a-z|0-9]";
    private final String stop_word = "\\s|的|，|:|：|、|-|,|;|；|<|>|\\(|\\)|（|）|\\[|\\]|“|”|/|\\\\|\\+|=|_|\\|";

    private long all_word_cnt = 0l;
    private final List<String> sentences = new ArrayList<>();
    private final Map<String, Long> word_cnt_map = new HashMap<>();

    private final Logger log = LoggerFactory.getLogger(WordTool.class);

    public static void main(String[] args) {
        WordTool t = new WordTool();
        t.initMetadatas();
        t.splitWord();
    }

    private void initMetadatas() {
        try (FileReader is = new FileReader("./table_metadatas.txt");
             BufferedReader input = new BufferedReader(is)) {
            input.lines().forEach(content -> {
                String[] fields = content.split(",", 4);

                MetadataBean bean = new MetadataBean();
                bean.setFieldNameCn(fields[3]);
                bean.setFieldNameEn(fields[2]);
                bean.setTableNameCn(fields[1]);
                bean.setTableNameEn(fields[0]);


                if (Datas.tables.containsKey(bean.getTableNameEn())) {
                    Datas.tables.get(bean.getTableNameEn()).add(bean);
                } else {
                    List<MetadataBean> table = new ArrayList<>();
                    table.add(bean);
                    Datas.tables.put(bean.getTableNameEn(), table);
                }
            });

            log.info("===table count:" + Datas.tables.size());

        } catch (Exception e) {
            log.error("=== initMetadatas error:", e);
        }
    }

    private void splitWord() {
        for (Map.Entry<String, List<MetadataBean>> item : Datas.tables.entrySet()) {
            for (MetadataBean bean : item.getValue()) {
                if (Strings.notNullOrEmpty(bean.getFieldNameCn())) {
                    sentences.add(bean.getFieldNameCn());
                }
                if (Strings.notNullOrEmpty(bean.getTableNameCn())) {
                    sentences.add(bean.getTableNameCn());
                }
                if (Strings.notNullOrEmpty(bean.getTableNameEn())) {
                    sentences.add(bean.getTableNameEn());
                }
                if (Strings.notNullOrEmpty(bean.getFieldNameEn())) {
                    sentences.add(bean.getFieldNameEn());
                }
            }
        }

        setWordCnt(sentences);

        log.info(String.format("===all word count:%d", all_word_cnt));

        Set<String> myWord = new HashSet<>();

        String term = null;
        String longTerm = null;
        String word = null;
        StringBuilder tmpTerm = new StringBuilder();
        StringBuilder tmpLongTerm = new StringBuilder();
        int m = 0;
        float p_one = p_one();

        for (String sentence : sentences) {

            getEnglishWords(sentence);

            sentence = sentence.replaceAll(regex, "").replaceAll(stop_word, "");
            String[] words = sentence.split("");

            String lastWord = "";
            float lastWordRate = 0.0f;

            for (int i = 0; i < words.length - 1; i++) {
                word = words[i];
                term = tmpTerm.append(words[i]).append(words[i + 1]).toString();

                if (i + 2 < words.length) {
                    longTerm = tmpLongTerm.append(words[i]).append(words[i + 1]).append(words[i + 2]).toString();
                } else {
                    longTerm = "";
                }

                tmpTerm.delete(0, tmpTerm.length());
                tmpLongTerm.delete(0, tmpLongTerm.length());

                float p_word = p_word(word);

                float p_term_word = p_term_word(term, word);
                float p_term = p_term(term);

                float termRate = p_term_word * p_word / p_term;

                float ltermRate = 0.0f;
                if (longTerm.length() > 0) {
                    float p_lterm_word = p_term_word(longTerm, word);
                    float p_lterm = p_term(longTerm);
                    ltermRate = p_lterm_word * p_word / p_lterm;
                }

                String newWord = "";
                float newWordRate = 0.0f;

                if (termRate > ltermRate) {
                    newWord = term;
                    newWordRate = termRate;
                } else if (ltermRate > 0 && p_one != p_term) {
                    Map<String, Float> tlterm = getLongWord(words, word, i, ltermRate, p_word);
                    for (Map.Entry<String, Float> item : tlterm.entrySet()) {
                        if (item.getKey().length() > 0) {
                            newWord = item.getKey();
                            newWordRate = item.getValue().floatValue();
                        } else {
                            newWord = longTerm;
                            newWordRate = ltermRate;
                        }
                    }
                } else if (p_one == p_term) {
                    newWord = term;
                    newWordRate = termRate;
                }

                String lastW = "", newW = "", lastLW = "", newLW = "";
                if (lastWord.length() > 0) {
                    lastW = lastWord.substring(lastWord.length() - 1);
                    lastLW = lastWord.substring(0, 1);
                }
                if (newWord.length() > 0) {
                    newW = newWord.substring(0, 1);
                    newLW = newWord.substring(newWord.length() - 1);
                }

                if ((lastW.length() > 0 && lastW.equals(newW))
                        || (lastLW.length() > 0 && lastLW.equals(newLW))
                        || lastWord.contains(newWord)
                        || newWord.contains(lastWord)) {
                    if (newWordRate > (lastWordRate + 0.005)) {
                        myWord.add(newWord);
                        lastWord = newWord;
                        lastWordRate = newWordRate;

                        log.info(newWord);
                    }
                } else {
                    myWord.add(newWord);
                    lastWord = newWord;
                    lastWordRate = newWordRate;

                    log.info(newWord);
                }
            }
        }

        myWord.addAll(english_word);

        saveFile("./word_cnt.txt", myWord);
    }


    private void saveFile(String fileName, Set<String> words) {
        try (FileOutputStream f = new FileOutputStream(new File(fileName));
             DataOutputStream output = new DataOutputStream(f)) {
            StringBuilder row = new StringBuilder();
            for (String word : words) {
                long word_cnt = getTermCnt(word);
                row.append(word).append(",").append(word_cnt).append("\r\n");
                String rowStr = row.toString();
                row.delete(0, row.length());

                output.write(rowStr.getBytes("UTF-8"));
            }
            output.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Map<String, Float> getLongWord(String[] words, String rootWord, int i, float tRate, float p_word) {
        Map<String, Float> wordInfo = new HashMap<>();

        int add = 3;
        String word = "";
        float wordRate = 0.0f;
        float targetRate = tRate;
        int index = i;

        String longTerm = "";
        StringBuilder tmpLongTerm = new StringBuilder();

        while (index + add < words.length) {
            int tmpAdd = 0;
            while (tmpAdd <= add) {
                tmpLongTerm.append(words[index + tmpAdd]);
                tmpAdd++;
            }
            longTerm = tmpLongTerm.toString();
            tmpLongTerm.delete(0, tmpLongTerm.length());

            float ltermRate = 0.0f;
            if (longTerm.length() > 0) {
                float p_lterm_word = p_term_word(longTerm, rootWord);
                float p_lterm = p_term(longTerm);
                ltermRate = p_lterm_word * p_word / p_lterm;
            }

            if (ltermRate > 0 && ltermRate >= targetRate) {
                word = longTerm;
                wordRate = ltermRate;
                targetRate = ltermRate;
            } else {
                break;
            }

            add++;
        }

        wordInfo.put(word, wordRate);

        return wordInfo;
    }

    //P(词根)
    private float p_word(String word) {
        float rate = 0.0f;
        long word_cnt = word_cnt_map.get(word);
        rate = 1.0f * word_cnt / (all_word_cnt + 1);

        return rate;
    }

    //P(词|词根)
    private float p_term_word(String term, String word) {
        float rate = 0.0f;
        long term_cnt = getTermCnt(term);
        long word_cnt = word_cnt_map.get(word);
        rate = 1.0f * term_cnt / (word_cnt + 1);

        return rate;
    }

    //P(词)
    private float p_term(String term) {
        float rate = 0.0f;
        long term_cnt = getTermCnt(term);
        rate = 1.0f * (term_cnt + 1) / (sentences.size() + 1);

        return rate;
    }

    //P(1)
    private float p_one() {
        float rate = 0.0f;
        rate = 2.0f / (sentences.size() + 1);
        return rate;
    }

    private long getTermCnt(String term) {
        long term_cnt = 0;
        for (String sentence : sentences) {
            if (sentence.contains(term)) {
                term_cnt++;
            }
        }
        return term_cnt;
    }

    private void getEnglishWords(String sentence) {
        String[] words = sentence.split("");
        StringBuilder englishWord = new StringBuilder();
        for (String word : words) {
            if (word.matches(regex)) {
                englishWord.append(word);
            } else {
                if (englishWord.length() > 0) {
                    english_word.add(englishWord.toString());
                    englishWord.delete(0, englishWord.length());
                }
            }
        }
        if (englishWord.length() > 0) {
            english_word.add(englishWord.toString());
            englishWord.delete(0, englishWord.length());
        }
    }

    private Map<String, Long> setWordCnt(List<String> sentences) {
        for (String sentence : sentences) {
            sentence = sentence.replaceAll(regex, "");
            String[] words = sentence.split("");
            all_word_cnt += words.length;
            for (String word : words) {
                if (word_cnt_map.containsKey(word)) {
                    long cnt = word_cnt_map.get(word) + 1;
                    word_cnt_map.put(word, cnt);
                } else {
                    word_cnt_map.put(word, 1l);
                }
            }
        }
        return word_cnt_map;
    }
}
