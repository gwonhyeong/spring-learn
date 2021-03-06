package springbook;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.aop.framework.ProxyFactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import springbook.user.dao.UserDao;
import springbook.user.domain.Level;
import springbook.user.domain.User;
import springbook.user.service.*;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.*;

@Slf4j
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AppContext.class)
@ActiveProfiles("test")
public class UserServiceTest {
    @Autowired
    UserService userService;

    @Autowired
    DataSource dataSource;

    @Autowired
    UserDao userDao;

    @Autowired
    MailSender mailSender;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    ApplicationContext applicationContext;

    @Test
    public void bean(){
        assertThat( this.userService , is(notNullValue()) );
        assertThat( this.dataSource , is(notNullValue()) );
        assertThat( this.userDao , is(notNullValue()) );
        assertThat( this.mailSender , is(notNullValue()));
        assertThat( this.transactionManager , is(notNullValue()));
    }

    List<User> userList;
    @Before
    public void setUp(){
        userList = Arrays.asList(
                new User("1", "Name 1", "Password 1"
                        , Level.BASIC , 49 , 0 )
                , new User("2", "Name 2", "Password 2"
                        , Level.BASIC , 50 , 0 )
                , new User("3", "Name 3", "Password 3"
                        , Level.SILVER , 60 , 29 )
                , new User("4", "Name 4", "Password 4"
                        , Level.SILVER , 60 , 30 )
                , new User("5", "Name 5", "Password 5"
                        , Level.GOLD , 100 , 100 )
        );
    }


    @Test
    public void upgradeLevels() throws SQLException {
        userDao.deleteAll();
        userList.forEach(u->userService.add(u));

        userService.upgradeLevels();

        List<Level> levels = Arrays.asList(Level.BASIC
                , Level.SILVER
                , Level.SILVER
                , Level.GOLD
                , Level.GOLD);

        IntStream.range(0,5).forEach( i -> assertEquals(
                    userDao.get( userList.get(i).getId()).getLevel() , levels.get(i)
                )
        );
    }

    @Test
    public void add(){
        userDao.deleteAll();
        User u = userList.get(4);
        u.setLevel(null);
        userService.add( u );

        assertEquals(userDao.get( u.getId() ).getLevel() , Level.BASIC);
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class TestUserServiceImpl extends UserServiceImpl {
        private final String id;

        public TestUserServiceImpl( String id ) {
            this.id = id;
        }

        @Override
        protected void upgradeLevel(User user) {
            if( user.getId().equals(this.id))
                throw new TestUserServiceException();
            super.upgradeLevel(user);
        }

        @Override
        public List<User> getAll(){
            super.getAll().forEach(u->super.update(u));
            return null;
        }
    }

    @Autowired
    UserService testUserService;
//
//    @Test
//    public void upgradeAllOrNothing() throws SQLException {
//        userDao.deleteAll();
//        userList.stream().forEach(u->userService.add(u));
//        TestUserService service = new TestUserService(userList.get(3).getId());
//        service.setUserDao(userDao);
//        service.setDataSource(dataSource);
//        service.setMailSender(mailSender);
//        UserServiceTx txUserService = new UserServiceTx();
//        txUserService.setTransactionManager(transactionManager);
//        txUserService.setUserService(service);
//        try {
//            txUserService.upgradeLevels();
//        }catch (TestUserServiceException e) {
//        }
//        List<Level> levels = Arrays.asList(Level.BASIC
//                , Level.BASIC
//                , Level.SILVER
//                , Level.SILVER
//                , Level.GOLD);
//
//        IntStream.range(0,5).forEach( i -> {
//            assertEquals(userDao.get( userList.get(i).getId()).getLevel() , levels.get(i));
//        });
//    }


//    @Test
//    @DirtiesContext
//    public void upgradeAllOrNothing() throws SQLException {
//        userDao.deleteAll();
//        userList.forEach(u->userService.add(u));
//        TestUserService service = new TestUserService(userList.get(3).getId());
//        service.setUserDao(userDao);
//        service.setDataSource(dataSource);
//        service.setMailSender(mailSender);
//
//        TransactionHandler txHandler = new TransactionHandler();
//        txHandler.setTarget(service);
//        txHandler.setTransactionManager(this.transactionManager);
//        txHandler.setPattern("upgradeLevels");
//
//        UserService txUserService = (UserService) Proxy.newProxyInstance(
//                getClass().getClassLoader()
//                , new Class[] { UserService.class }
//                , txHandler
//        );
//
//        try {
//            txUserService.upgradeLevels();
//        }catch (TestUserServiceException e) {
//        }
//        List<Level> levels = Arrays.asList(Level.BASIC
//                , Level.BASIC
//                , Level.SILVER
//                , Level.SILVER
//                , Level.GOLD);
//
//        IntStream.range(0,5).forEach( i -> assertEquals(userDao.get( userList.get(i).getId()).getLevel() , levels.get(i)));
//    }

//    @Test
//    @DirtiesContext
//    public void upgradeAllOrNothing() throws Exception {
//        userDao.deleteAll();
//        userList.forEach(u->userService.add(u));
//        TestUserService service = new TestUserService(userList.get(3).getId());
//        service.setUserDao(userDao);
//        service.setDataSource(dataSource);
//        service.setMailSender(mailSender);
///*
//        TransactionProxyFactoryBean factory = this.applicationContext.getBean("&userService"
//                , TransactionProxyFactoryBean.class );
//*/
//        ProxyFactoryBean factory = this.applicationContext.getBean("&userService"
//                ,ProxyFactoryBean.class);
//        factory.setTarget(service);
//
//        UserService txUserService = (UserService) factory.getObject();
//
//        try {
//            txUserService.upgradeLevels();
//        }catch (TestUserServiceException e) {
//        }
//        List<Level> levels = Arrays.asList(Level.BASIC
//                , Level.BASIC
//                , Level.SILVER
//                , Level.SILVER
//                , Level.GOLD);
//
//        IntStream.range(0,5).forEach( i -> assertEquals(userDao.get( userList.get(i).getId()).getLevel() , levels.get(i)));
//    }

    @Test
    public void testServiceIsProxy(){
        assertEquals(testUserService.getClass().getSimpleName().contains("Proxy") , true);
    }
    @Test
    public void upgradeAllOrNothing() throws Exception {
        userDao.deleteAll();
        userList.forEach(u->userService.add(u));

        try {
            testUserService.upgradeLevels();
        }catch (TestUserServiceException e) {
        }
        List<Level> levels = Arrays.asList(Level.BASIC
                , Level.BASIC
                , Level.SILVER
                , Level.SILVER
                , Level.GOLD);

        IntStream.range(0,5).forEach( i -> assertEquals(userDao.get( userList.get(i).getId()).getLevel() , levels.get(i)));
    }

    @Test(expected = TransientDataAccessResourceException.class )
    public void readOnlyTransactionApplyTest(){
        userDao.deleteAll();
        userList.forEach(u->userService.add(u));
        testUserService.getAll();
    }
}
