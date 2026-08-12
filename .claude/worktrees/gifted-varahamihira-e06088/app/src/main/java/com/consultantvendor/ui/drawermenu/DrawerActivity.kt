package com.consultantvendor.ui.drawermenu

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.consultantvendor.R
import com.consultantvendor.data.repos.UserRepository
import com.consultantvendor.databinding.ActivityContainerBinding
import com.consultantvendor.ui.chat.ChatFragment
import com.consultantvendor.ui.dashboard.feeds.AddFeedFragment
import com.consultantvendor.ui.dashboard.feeds.FeedDetailsFragment
import com.consultantvendor.ui.dashboard.feeds.FeedsFragment
import com.consultantvendor.ui.dashboard.home.AppointmentFragment
import com.consultantvendor.ui.dashboard.home.appointment.detail.AppointmentDetailsFragment
import com.consultantvendor.ui.dashboard.home.healthtool.bmichecker.BmiCheckerFragment
import com.consultantvendor.ui.dashboard.home.healthtool.pregnancycalculator.PregnancyCalculatorFragment
import com.consultantvendor.ui.dashboard.home.healthtool.protienintake.ProteinIntakeFragment
import com.consultantvendor.ui.dashboard.home.healthtool.waterintake.WaterIntakeFragment
import com.consultantvendor.ui.dashboard.home.prescription.digital.DigitalPrescriptionFragment
import com.consultantvendor.ui.dashboard.home.prescription.manual.ManualPrescriptionFragment
import com.consultantvendor.ui.dashboard.home.questions.QuestionsFragment
import com.consultantvendor.ui.dashboard.home.questions.detail.QuestionDetailFragment
import com.consultantvendor.ui.dashboard.home.appointment.patientfile.PatientFileFragment
import com.consultantvendor.ui.dashboard.home.reports.AddReportFragment
import com.consultantvendor.ui.dashboard.home.reports.AddReportNewFragment
import com.consultantvendor.ui.dashboard.language.LanguageFragment
import com.consultantvendor.ui.dashboard.location.LocationFragment
import com.consultantvendor.ui.dashboard.settings.contactlist.ContactListFragment
import com.consultantvendor.ui.dashboard.success.NetworkIssueFragment
import com.consultantvendor.ui.dashboard.wallet.PayoutFragment
import com.consultantvendor.ui.dashboard.wallet.WalletFragment
import com.consultantvendor.ui.dashboard.wallet.addmoney.AddCardFragment
import com.consultantvendor.ui.drawermenu.classes.ClassesFragment
import com.consultantvendor.ui.drawermenu.history.HistoryFragment
import com.consultantvendor.ui.drawermenu.notification.NotificationFragment
import com.consultantvendor.ui.drawermenu.profile.ProfileFragment
import com.consultantvendor.ui.loginSignUp.changepassword.ChangePasswordFragment
import com.consultantvendor.ui.walkthrough.WalkThroughFragment
import com.consultantvendor.utils.*
import dagger.android.support.DaggerAppCompatActivity
import javax.inject.Inject

class DrawerActivity : DaggerAppCompatActivity() {

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var prefsManager: PrefsManager

    lateinit var binding: ActivityContainerBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_container)
        setContentView(binding.root)
        applyInsets(binding.root)

        initialise()
    }

    private fun initialise() {
        LocaleHelper.setLocale(this, userRepository.getUserLanguage(), prefsManager)




        when (intent.getStringExtra(PAGE_TO_OPEN)) {
            NETWORK_ERROR -> addFragment(NetworkIssueFragment())
            HISTORY -> addFragment(HistoryFragment())
            CHANGE_PASSWORD -> addFragment(ChangePasswordFragment())
            LANGUAGE_SCREEN -> addFragment(LanguageFragment())
            PROFILE -> addFragment(ProfileFragment())
            NOTIFICATION -> addFragment(NotificationFragment())
            USER_CHAT -> addFragment(ChatFragment())
            PAYOUT, BANK_INFO -> addFragment(PayoutFragment())
            WALLET -> addFragment(WalletFragment())
            CLASSES -> addFragment(ClassesFragment())
            ADD_CARD -> addFragment(AddCardFragment())
            APPOINTMENT -> addFragment(AppointmentFragment())
            APPOINTMENT_DETAILS -> addFragment(AppointmentDetailsFragment())
            BlogType.BLOG, BlogType.ARTICLE -> addFragment(FeedsFragment())
            ADD_ARTICLE, ADD_BLOG -> addFragment(AddFeedFragment())
            BLOGS_DETAILS -> {
                val fragment = FeedDetailsFragment()
                val bundle = Bundle()
                bundle.putSerializable(EXTRA_REQUEST_ID, intent.getSerializableExtra(EXTRA_REQUEST_ID))
                fragment.arguments = bundle
                addFragment(fragment)
            }
            PrescriptionType.MANUAL -> addFragment(ManualPrescriptionFragment())
            PrescriptionType.DIGITAL -> addFragment(DigitalPrescriptionFragment())
            WalkThroughFragment.WALK_THROUGH_SCREEN -> addFragment(WalkThroughFragment())
            LOCATION -> addFragment(LocationFragment())
            MY_QUESTION ->  addFragment(QuestionsFragment())
            QUESTION_DETAILS -> {
                val fragment = QuestionDetailFragment()
                val bundle = Bundle()
                bundle.putString(EXTRA_REQUEST_ID, intent.getStringExtra(EXTRA_REQUEST_ID))
                fragment.arguments = bundle

                addFragment(fragment)
            }
            BMI_CHECKER -> addFragment(BmiCheckerFragment())
            WATER_INTAKE -> addFragment(WaterIntakeFragment())
            PROTEIN_INTAKE -> addFragment(ProteinIntakeFragment())
            PREGNANCY_CALCULATOR -> addFragment(PregnancyCalculatorFragment())
            CONTACT_LIST -> addFragment(ContactListFragment())
            ADD_REPORTS -> addFragment(AddReportFragment())
            ADD_REPORT_NEW -> addFragment(AddReportNewFragment())
            PATIENT_FILE -> addFragment(PatientFileFragment())
        }
    }

    private fun addFragment(fragment: Fragment) {
        addFragment(supportFragmentManager, fragment, R.id.container)
    }

    companion object {
        const val NETWORK_ERROR = "NETWORK_ERROR"
        const val HISTORY = "HISTORY"
        const val CHANGE_PASSWORD = "CHANGE_PASSWORD"
        const val LANGUAGE_SCREEN = "LANGUAGE_SCREEN"
        const val PROFILE = "PROFILE"
        const val NOTIFICATION = "NOTIFICATION"
        const val USER_CHAT = "USER_CHAT"
        const val PAYOUT = "PAYOUT"
        const val WALLET="WALLET"
        const val CLASSES = "CLASSES"
        const val ADD_CARD = "ADD_CARD"
        const val APPOINTMENT = "APPOINTMENT"
        const val APPOINTMENT_DETAILS="APPOINTMENT_DETAILS"
        const val ADD_ARTICLE = "ADD_ARTICLE"
        const val ADD_BLOG = "ADD_BLOG"
        const val BLOGS_DETAILS = "BLOGS_DETAILS"
        const val LOCATION = "LOCATION"
        const val MY_QUESTION = "MY_QUESTION"
        const val QUESTION_DETAILS = "QUESTION_DETAILS"
        const val BMI_CHECKER = "BMI_CHECKER"
        const val WATER_INTAKE = "WATER_INTAKE"
        const val PROTEIN_INTAKE = "PROTEIN_INTAKE"
        const val PREGNANCY_CALCULATOR = "PREGNANCY_CALCULATOR"
        const val BANK_INFO = "BANK_INFO"
        const val CONTACT_LIST = "CONTACT_LIST"
        const val ADD_REPORTS = "ADD_REPORTS"
        const val ADD_REPORT_NEW = "ADD_REPORT_NEW"
        const val PATIENT_FILE = "PATIENT_FILE"
    }

}
