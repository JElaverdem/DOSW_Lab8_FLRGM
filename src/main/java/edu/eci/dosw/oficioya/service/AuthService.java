package edu.eci.dosw.oficioya.service;

import edu.eci.dosw.oficioya.dto.LoginRequest;
import edu.eci.dosw.oficioya.dto.LoginResponse;
import edu.eci.dosw.oficioya.dto.RegistroRequest;
import edu.eci.dosw.oficioya.dto.RegistroResponse;

/**
 * Contrato de autenticación. La implementación vive en {@link AuthServiceImpl}.
 */
public interface AuthService {

    RegistroResponse registrar(RegistroRequest request);

    LoginResponse login(LoginRequest request);
}
