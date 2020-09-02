package dao;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DBUtil
{
    private static final String URL = "jdbc:mysql://127.0.0.1:3306/java_image_server?characterEncoding=utf8&useSSL=true";
    private static final String USERNAME = "root";
    private static final String PASSWORD = "123456";

    private static volatile DataSource dataSource=null;
    private static  DataSource getDataSource()
    {
        //通过这个方法创建DataSource 的实例
        //线程安全单例模式
        if(dataSource==null)
        {
            synchronized (DBUtil.class)
            {
                if(dataSource==null)
                {
                    dataSource = new MysqlDataSource();
                    MysqlDataSource tmpDataSource = (MysqlDataSource)dataSource;
                    tmpDataSource.setURL(URL);
                    tmpDataSource.setUser(USERNAME);
                    tmpDataSource.setPassword(PASSWORD);
                }
            }
        }
        return dataSource;
    }
    public static Connection getConnection()
    {
        try
        {
            return (Connection) getDataSource().getConnection();
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public static void close (Connection connection, PreparedStatement statement, ResultSet resultSet)
    {
        //关闭顺序：先创建的后关闭
        try
        {
            if(resultSet!=null)
            {
                resultSet.close();
            }
            if(statement!=null)
            {
                statement.close();
            }
            if(connection!=null)
            {
                connection.close();
            }
        } catch (SQLException e)
        {
            e.printStackTrace();
        }
    }
}