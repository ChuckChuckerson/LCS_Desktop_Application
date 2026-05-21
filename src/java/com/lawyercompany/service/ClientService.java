package com.lawyercompany.service;

import com.lawyercompany.dao.ClientDAO;
import com.lawyercompany.entity.Client;
import com.lawyercompany.factory.DAOFactory;

import java.util.Optional;
import java.util.UUID;

public class ClientService {

    private final ClientDAO clientDAO;

    public ClientService() {
        this.clientDAO = DAOFactory.getClientDAO();
    }

    public Optional<Client> getClientByUserId(UUID userId) {
        return clientDAO.findByUserId(userId);
    }

    public Optional<Client> getClientById(UUID clientId) {
        return Optional.ofNullable(clientDAO.findById(clientId));
    }

    public void updateClient(Client client) {
        clientDAO.update(client);
    }
}