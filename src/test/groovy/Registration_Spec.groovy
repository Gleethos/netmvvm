import app.UserRegistrationViewModel
import spock.lang.Specification



class Registration_Spec extends Specification
{

    def 'The registration view model will display feedback about invalid inputs.'()
    {
        reportInfo """
            Note that when changing the value of a property we
            use the "act" method instead of the "set" method.
            This is because the "set" method is used to set the
            value of the property without triggering any
            validation logic. The "act" method represents
            a user action that triggers validation logic.
        """
        given : 'We create the view model.'
            var vm = new UserRegistrationViewModel()
        expect : 'Initially all the user inputs are empty.'
            vm.username().is("")
            vm.password().is("")
            vm.email().is("")
            vm.gender().is(UserRegistrationViewModel.Gender.NOT_SELECTED)
            vm.termsAccepted().is(false)
        and : 'The initial feedback on the other hand is not empty, tells the user what is missing.'
            vm.feedback().isNot("")
        and : 'It contains the expected messages!'
            vm.feedback().get().contains("Username must be at least 3 characters long")
            vm.feedback().get().contains("Password must be at least 8 characters long")
            vm.feedback().get().contains("Email must contain an @ character")
            vm.feedback().get().contains("You must select a valid gender")
            vm.feedback().get().contains("You must accept the terms and conditions")
        when : 'We set the username to a valid value.'
            vm.username().act("bob")
        then : 'The feedback is updated to reflect the change.'
            !vm.feedback().get().contains("Username must be at least 3 characters long")

        when : 'We set the password to a valid value.'
            vm.password().act("Password")
        then : 'The feedback is updated to reflect the change.'
            !vm.feedback().get().contains("Password must be at least 8 characters long")

        when : 'We set the email to a valid value.'
            vm.email().act("something@something.com")
        then : 'The feedback is updated to reflect the change.'
            !vm.feedback().get().contains("Email must contain an @ character")

        when : 'We set the gender to a valid value.'
            vm.gender().act(UserRegistrationViewModel.Gender.FEMALE)
        then : 'The feedback is updated to reflect the change.'
            !vm.feedback().get().contains("You must select a valid gender")

        when : 'We set the terms accepted to a valid value.'
            vm.termsAccepted().act(true)
        then : 'The feedback is updated to reflect the change.'
            !vm.feedback().get().contains("You must accept the terms and conditions")

        and : 'The feedback is now empty.'
            vm.feedback().is("All inputs are valid, feel fre to press the submit button!")
    }

    def 'The register button does nothing if the inputs are not all valid.'()
    {
        given : 'We create the view model.'
            var vm = new UserRegistrationViewModel()
        when : 'We press the register button.'
            vm.register()
        then : 'The register button is disabled.'
            !vm.successfullyRegistered()
    }
}
