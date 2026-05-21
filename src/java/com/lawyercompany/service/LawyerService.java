package com.lawyercompany.service;

import com.lawyercompany.dao.LawyerDAO;
import com.lawyercompany.dao.UserDAO;
import com.lawyercompany.entity.Lawyer;
import com.lawyercompany.entity.User;
import com.lawyercompany.factory.DAOFactory;
import com.lawyercompany.up.util.BCryptPasswordEncoder;
import com.lawyercompany.up.util.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class LawyerService {

    private final UserDAO userDAO;
    private final LawyerDAO lawyerDAO;
    private final UserService userService;

    public LawyerService() {
        this.userDAO = DAOFactory.getUserDAO();
        this.lawyerDAO = DAOFactory.getLawyerDAO();
        this.userService = new UserService();
    }

    /**
     * Получить список всех адвокатов (не удалённых).
     *
     * @return список адвокатов
     */
    public List<Lawyer> getAllLawyers() {
        return lawyerDAO.findAll();
    }

    /**
     * Найти профиль адвоката по идентификатору пользователя.
     *
     * @param userId идентификатор пользователя
     * @return профиль адвоката, если найден
     */
    public Optional<Lawyer> getLawyerByUserId(UUID userId) {
        return lawyerDAO.findByUserId(userId);
    }

    /**
     * Обновить профиль адвоката.
     *
     * @param lawyer профиль адвоката
     */
    public void updateLawyer(Lawyer lawyer) {
        lawyerDAO.update(lawyer);
    }

    /**
     * Получить ФИО адвоката по его {@code userId}.
     *
     * @param lawyer профиль адвоката
     * @return ФИО или строка-заглушка
     */
    public String getLawyerFullName(Lawyer lawyer) {
        return userService.getUserById(lawyer.getUserId())
                .map(User::getFullName)
                .orElse("Неизвестный адвокат");
    }

    /**
     * Создать нового адвоката (пользователь + профиль адвоката).
     *
     * @param username логин
     * @param rawPassword пароль в открытом виде
     * @param fullName ФИО
     * @param email email
     * @param phone телефон
     * @param specialization специализация
     * @param licenseNumber номер удостоверения
     * @param officeAddress адрес офиса
     * @param experienceYears стаж (лет)
     * @param avatarData аватар пользователя (может быть {@code null})
     */
    public void createLawyer(String username, String rawPassword, String fullName, String email,
                             String phone, String specialization, String licenseNumber,
                             String officeAddress, int experienceYears, byte[] avatarData) {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(encoder.encode(rawPassword));
        user.setFullName(fullName);
        user.setEmail(email);
        user.setRole(User.ROLE_LAWYER);
        user.setAvatarData(avatarData);
        user.setDeleted(false);
        user.setBlocked(false);
        userDAO.save(user);

        Lawyer lawyer = new Lawyer();
        lawyer.setUserId(user.getId());
        lawyer.setPhone(phone);
        lawyer.setSpecialization(specialization);
        lawyer.setLicenseNumber(licenseNumber);
        lawyer.setOfficeAddress(officeAddress);
        lawyer.setExperienceYears(experienceYears);
        lawyerDAO.save(lawyer);
    }
}