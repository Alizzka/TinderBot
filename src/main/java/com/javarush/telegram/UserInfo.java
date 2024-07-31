package com.javarush.telegram;

public class UserInfo {
    public String name; //Имя
    public String sex; //Пол
    public String age; //Возраст
    public String city; //Город
    public String occupation; //Профессия
    public String hobby; //Хобби
    public String handsome; //Красота, привлекательность
    public String wealth; //Доход, богатство
    public String annoys; //Меня раздражает в людях
    public String goals; //Цели знакомства

    private String fieldToString(String str, String description) {
        if (str != null && !str.isEmpty())
            return description + ": " + str + "\n";
        else
            return "";
    }

    @Override
    public String toString() {
        String result = "";

        result += fieldToString(name, "Имя");
        result += fieldToString(sex, "Пол");
        result += fieldToString(age, "Возраст");
        result += fieldToString(city, "Город");
        result += fieldToString(occupation, "Профессия");
        result += fieldToString(hobby, "Хобби");
        result += fieldToString(handsome, "Красота, привлекательность в баллах (максимум 10 баллов)");
        result += fieldToString(wealth, "Доход, богатство");
        result += fieldToString(annoys, "В людях раздражает");
        result += fieldToString(goals, "Цели знакомства");

        return result;
    }
}
