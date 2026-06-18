package mx.edu.cenidet.estadias.services.auth;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class PasswordPolicyValidator {

    private static final Pattern PATRON = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&_\\-])[A-Za-z\\d@$!%*?&_\\-]{8,}$"
    );

    public boolean esValida(String contrasenia) {
        return contrasenia != null && PATRON.matcher(contrasenia).matches();
    }

    public void validarOLanzar(String contrasenia) {
        if (!esValida(contrasenia)) {
            throw new mx.edu.cenidet.estadias.excepciones.BusinessRuleException(
                    "La contraseña debe tener al menos 8 caracteres, 1 mayúscula, "
                            + "1 minúscula, 1 número y 1 carácter especial (@$!%*?&_-).");
        }
    }
}