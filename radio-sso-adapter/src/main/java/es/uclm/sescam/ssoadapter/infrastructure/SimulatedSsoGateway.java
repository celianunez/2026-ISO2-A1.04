package es.uclm.sescam.ssoadapter.infrastructure;

import es.uclm.sescam.ssoadapter.api.CorporateIdentityDto;
import es.uclm.sescam.ssoadapter.api.ISsoGateway;
import es.uclm.sescam.ssoadapter.api.InvalidSsoTokenException;
import es.uclm.sescam.ssoadapter.api.SsoUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Simulated corporate SSO (D-08, D-27): for now the only {@link ISsoGateway} of the application. When a real adapter
 * is added, both will need a condition to choose one.
 *
 * <p>It knows the fictitious users agreed with CU-18 (the same ids as the seed users of {@code radio-auth}). The token
 * of a user is {@code token-<userId>}; {@code token-expired} and any other token are rejected. With
 * {@code radio.sso.simulator.down=true} every call fails as if the SSO could not be reached.
 */
@Component
public class SimulatedSsoGateway implements ISsoGateway {

    static final String TOKEN_PREFIX = "token-";

    static final String EXPIRED_TOKEN = "token-expired";

    static final List<CorporateIdentityDto> USERS = List.of(
            new CorporateIdentityDto("patient01", "Demo Patient One"),
            new CorporateIdentityDto("patient02", "Demo Patient Two"),
            new CorporateIdentityDto("physician01", "Demo Physician One"),
            new CorporateIdentityDto("physician02", "Demo Physician Two"),
            new CorporateIdentityDto("admissions01", "Demo Admissions One"),
            new CorporateIdentityDto("admin01", "Demo Administrator"),
            new CorporateIdentityDto("physicianadmin01", "Demo Physician Administrator"),
            new CorporateIdentityDto("norole01", "Demo No Role"));

    private final Map<String, CorporateIdentityDto> identitiesByToken = new LinkedHashMap<>();

    private final boolean down;

    /**
     * Creates the simulator.
     *
     * @param down {@code true} to simulate an SSO that cannot be reached (property {@code radio.sso.simulator.down})
     */
    public SimulatedSsoGateway(@Value("${radio.sso.simulator.down:false}") boolean down) {
        this.down = down;
        USERS.forEach(identity -> identitiesByToken.put(TOKEN_PREFIX + identity.userId(), identity));
    }

    @Override
    public CorporateIdentityDto authenticate(String token) {
        failIfDown();
        return find(token).orElseThrow(() -> new InvalidSsoTokenException(EXPIRED_TOKEN.equals(token)
                ? "The SSO token has expired"
                : "The SSO token is not recognised"));
    }

    @Override
    public Optional<CorporateIdentityDto> getCorporateIdentity(String token) {
        failIfDown();
        return find(token);
    }

    private Optional<CorporateIdentityDto> find(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(identitiesByToken.get(token));
    }

    private void failIfDown() {
        if (down) {
            throw new SsoUnavailableException("The corporate SSO cannot be reached (simulator in failure mode)");
        }
    }
}
