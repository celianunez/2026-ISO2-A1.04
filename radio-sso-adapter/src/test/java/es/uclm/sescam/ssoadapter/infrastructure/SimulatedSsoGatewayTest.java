package es.uclm.sescam.ssoadapter.infrastructure;

import es.uclm.sescam.ssoadapter.api.CorporateIdentityDto;
import es.uclm.sescam.ssoadapter.api.ISsoGateway;
import es.uclm.sescam.ssoadapter.api.InvalidSsoTokenException;
import es.uclm.sescam.ssoadapter.api.SsoUnavailableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contract test of {@link ISsoGateway} on its simulator (CU-20-S, test cases 1 to 3).
 */
class SimulatedSsoGatewayTest {

    private final ISsoGateway sso = new SimulatedSsoGateway(false);

    @ParameterizedTest
    @CsvSource({
        "patient01, Demo Patient One",
        "patient02, Demo Patient Two",
        "physician01, Demo Physician One",
        "physician02, Demo Physician Two",
        "admissions01, Demo Admissions One",
        "admin01, Demo Administrator",
        "physicianadmin01, Demo Physician Administrator",
        "norole01, Demo No Role"
    })
    void authenticatesEveryFictitiousUserWithTheirToken(String userId, String displayName) {
        CorporateIdentityDto identity = sso.authenticate("token-" + userId);

        assertThat(identity).isEqualTo(new CorporateIdentityDto(userId, displayName));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "token-unknown", "patient01", "TOKEN-PATIENT01"})
    void rejectsATokenItDoesNotKnow(String token) {
        assertThatThrownBy(() -> sso.authenticate(token))
                .isInstanceOf(InvalidSsoTokenException.class)
                .hasMessage("The SSO token is not recognised");
    }

    @Test
    void rejectsAnExpiredToken() {
        assertThatThrownBy(() -> sso.authenticate("token-expired"))
                .isInstanceOf(InvalidSsoTokenException.class)
                .hasMessage("The SSO token has expired");
    }

    @Test
    void returnsTheIdentityOfAValidTokenWithoutFailing() {
        assertThat(sso.getCorporateIdentity("token-admin01"))
                .contains(new CorporateIdentityDto("admin01", "Demo Administrator"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"token-unknown", "token-expired"})
    void returnsNoIdentityForAnInvalidToken(String token) {
        assertThat(sso.getCorporateIdentity(token)).isEmpty();
    }

    @Test
    void failsOnEveryCallWhenTheSsoIsDown() {
        ISsoGateway downSso = new SimulatedSsoGateway(true);

        assertThatThrownBy(() -> downSso.authenticate("token-patient01"))
                .isInstanceOf(SsoUnavailableException.class);
        assertThatThrownBy(() -> downSso.getCorporateIdentity("token-patient01"))
                .isInstanceOf(SsoUnavailableException.class);
    }
}
