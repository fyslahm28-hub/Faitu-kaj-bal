package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppNotification
import com.example.data.model.BookmarkItem
import com.example.data.model.DirectMessage
import com.example.data.model.FeedPost
import com.example.data.model.KhedmotSubmission
import com.example.data.model.MadrasaProfile
import com.example.data.model.UserProfile
import com.example.data.repository.QawmiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class MainDestination {
    HOME,
    FEED,
    KHEDMOT,
    ABEDON,
    MADRASA,
    PROFILE,
    BOOKMARKS,
    VERIFIED_MADRASAS,
    PUBLISH_CIRCULAR,
    PUBLISH_APPLICATION,
    REGISTER_MADRASA,
    MESSAGES,
    NOTIFICATIONS,
    ADMIN_PANEL,
    PRAYER_SCHEDULE
}

enum class FeedFilterTab {
    ALL,            // সব পোস্ট
    JOB_CIRCULAR,   // নিয়োগ বিজ্ঞপ্তি
    APPLICATIONS,   // আবেদন
    DISTRICT        // জেলা
}

class QawmiViewModel(
    private val repository: QawmiRepository,
    val currentUserId: String,
    val currentUserEmail: String,
    val currentUserDisplayName: String
) : ViewModel() {

    private val _currentDestination = MutableStateFlow(MainDestination.HOME)
    val currentDestination: StateFlow<MainDestination> = _currentDestination.asStateFlow()

    private val _navigationStack = ArrayDeque<MainDestination>()

    private val _selectedFeedFilter = MutableStateFlow(FeedFilterTab.ALL)
    val selectedFeedFilter: StateFlow<FeedFilterTab> = _selectedFeedFilter.asStateFlow()

    private val _selectedDistrictFilter = MutableStateFlow("সকল জেলা")
    val selectedDistrictFilter: StateFlow<String> = _selectedDistrictFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _feedbackBanner = MutableStateFlow<String?>(null)
    val feedbackBanner: StateFlow<String?> = _feedbackBanner.asStateFlow()

    private val _errorBanner = MutableStateFlow<String?>(null)
    val errorBanner: StateFlow<String?> = _errorBanner.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    // Active dialog / detail targets
    private val _selectedPostForApply = MutableStateFlow<FeedPost?>(null)
    val selectedPostForApply: StateFlow<FeedPost?> = _selectedPostForApply.asStateFlow()

    private val _selectedPostForDetail = MutableStateFlow<FeedPost?>(null)
    val selectedPostForDetail: StateFlow<FeedPost?> = _selectedPostForDetail.asStateFlow()

    private val _messageTargetUser = MutableStateFlow<Pair<String, String>?>(null) // (uid, name)
    val messageTargetUser: StateFlow<Pair<String, String>?> = _messageTargetUser.asStateFlow()

    // Real-time Firestore StateFlows (Two-tier error handling)
    val userProfileState: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing user profile", e)
            emit(UiState.Error(e.localizedMessage ?: "প্রোফাইল লোড করা যায়নি"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val feedPostsState: StateFlow<UiState<List<FeedPost>>> = repository.observePublicFeedPosts()
        .map<List<FeedPost>, UiState<List<FeedPost>>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing feed posts", e)
            emit(UiState.Error(e.localizedMessage ?: "ফিড লোড করা যায়নি"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val bookmarksState: StateFlow<UiState<List<BookmarkItem>>> = repository.observeBookmarks(currentUserId)
        .map<List<BookmarkItem>, UiState<List<BookmarkItem>>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing bookmarks", e)
            emit(UiState.Error(e.localizedMessage ?: "পছন্দের খেদমত লোড করা যায়নি"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val madrasasState: StateFlow<UiState<List<MadrasaProfile>>> = repository.observePublicMadrasas()
        .map<List<MadrasaProfile>, UiState<List<MadrasaProfile>>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing madrasas", e)
            emit(UiState.Error(e.localizedMessage ?: "মাদরাসার তালিকা লোড করা যায়নি"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val mySentSubmissionsState: StateFlow<UiState<List<KhedmotSubmission>>> = repository.observeMySentSubmissions(currentUserId)
        .map<List<KhedmotSubmission>, UiState<List<KhedmotSubmission>>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing sent submissions", e)
            emit(UiState.Error(e.localizedMessage ?: "আবেদন তালিকা লোড করা যায়নি"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val receivedSubmissionsState: StateFlow<UiState<List<KhedmotSubmission>>> = repository.observeReceivedSubmissions(currentUserId)
        .map<List<KhedmotSubmission>, UiState<List<KhedmotSubmission>>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing received submissions", e)
            emit(UiState.Error(e.localizedMessage ?: "প্রাপ্ত আবেদন লোড করা যায়নি"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val messagesState: StateFlow<UiState<List<DirectMessage>>> = repository.observeUserMessages(currentUserId)
        .map<List<DirectMessage>, UiState<List<DirectMessage>>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing messages", e)
            emit(UiState.Error(e.localizedMessage ?: "মেসেজ লোড করা যায়নি"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val notificationsState: StateFlow<UiState<List<AppNotification>>> = repository.observeNotifications(currentUserId)
        .map<List<AppNotification>, UiState<List<AppNotification>>> { UiState.Success(it) }
        .catch { e ->
            Log.w(TAG, "Error observing notifications", e)
            emit(UiState.Error(e.localizedMessage ?: "নোটিফিকেশন লোড করা যায়নি"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val isAdminUser: Boolean
        get() = currentUserEmail.equals("fyslahm28@gmail.com", ignoreCase = true)

    fun navigateTo(destination: MainDestination) {
        if (_currentDestination.value != destination) {
            _navigationStack.addLast(_currentDestination.value)
            _currentDestination.value = destination
        }
    }

    fun navigateBottomTab(destination: MainDestination) {
        _navigationStack.clear()
        _currentDestination.value = destination
    }

    fun navigateBack(): Boolean {
        return if (_navigationStack.isNotEmpty()) {
            _currentDestination.value = _navigationStack.removeLast()
            true
        } else if (_currentDestination.value != MainDestination.HOME) {
            _currentDestination.value = MainDestination.HOME
            true
        } else {
            false
        }
    }

    fun setFeedFilter(tab: FeedFilterTab) {
        _selectedFeedFilter.value = tab
    }

    fun setDistrictFilter(district: String) {
        _selectedDistrictFilter.value = district
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openFeedWithFilter(tab: FeedFilterTab) {
        _selectedFeedFilter.value = tab
        navigateTo(MainDestination.FEED)
    }

    fun openApplyDialog(post: FeedPost?) {
        _selectedPostForApply.value = post
    }

    fun openPostDetailDialog(post: FeedPost?) {
        _selectedPostForDetail.value = post
    }

    fun openDirectMessageTo(userId: String, userName: String) {
        _messageTargetUser.value = userId to userName
        navigateTo(MainDestination.MESSAGES)
    }

    fun clearMessageTarget() {
        _messageTargetUser.value = null
    }

    fun clearBanners() {
        _feedbackBanner.value = null
        _errorBanner.value = null
    }

    fun saveProfile(
        fullName: String,
        accountType: String,
        district: String,
        designation: String,
        qualification: String,
        experience: String,
        preferredDistricts: String,
        bio: String,
        contactInfo: String,
        madrasaName: String,
        onComplete: () -> Unit = {}
    ) {
        if (fullName.isBlank()) {
            _errorBanner.value = "অনুগ্রহ করে আপনার পূর্ণ নাম উল্লেখ করুন।"
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            clearBanners()
            val profile = UserProfile(
                userId = currentUserId,
                fullName = fullName.trim(),
                accountType = accountType,
                district = district.trim(),
                designation = designation.trim(),
                qualification = qualification.trim(),
                experience = experience.trim(),
                preferredDistricts = preferredDistricts.trim(),
                bio = bio.trim(),
                contactInfo = contactInfo.trim(),
                madrasaName = madrasaName.trim()
            )
            val result = repository.saveUserProfile(profile)
            _isSubmitting.value = false
            result.onSuccess {
                _feedbackBanner.value = "আলহামদুলিল্লাহ, আপনার প্রোফাইল তথ্য সংরক্ষিত হয়েছে।"
                onComplete()
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "প্রোফাইল সংরক্ষণে ত্রুটি হয়েছে।"
            }
        }
    }

    fun publishFeedPost(
        authorName: String,
        postType: String,
        title: String,
        district: String,
        upazilaOrLocation: String,
        designation: String,
        qualification: String,
        salaryOrExperience: String,
        deadlineOrPreferredDistricts: String,
        description: String,
        contactNumber: String,
        onSuccessNavigate: () -> Unit = {}
    ) {
        if (title.isBlank() || district.isBlank() || designation.isBlank() || qualification.isBlank()) {
            _errorBanner.value = "অনুগ্রহ করে শিরোনাম, জেলা, পদ এবং শিক্ষাগত যোগ্যতা পূরণ করুন।"
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            clearBanners()
            val result = repository.createFeedPost(
                authorName = authorName.ifBlank { title },
                postType = postType,
                title = title,
                district = district,
                upazilaOrLocation = upazilaOrLocation.ifBlank { district },
                designation = designation,
                qualification = qualification,
                salaryOrExperience = salaryOrExperience.ifBlank { "আলোচনা সাপেক্ষ" },
                deadlineOrPreferredDistricts = deadlineOrPreferredDistricts.ifBlank { "চলমান" },
                description = description,
                contactNumber = contactNumber
            )
            _isSubmitting.value = false
            result.onSuccess {
                _feedbackBanner.value = if (postType == "JOB_CIRCULAR") {
                    "আলহামদুলিল্লাহ, খেদমতের নিয়োগ বিজ্ঞপ্তিটি ফিডে প্রকাশিত হয়েছে।"
                } else {
                    "আলহামদুলিল্লাহ, আপনার খেদমতের আবেদনটি ফিডে প্রকাশিত হয়েছে।"
                }
                onSuccessNavigate()
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "প্রকাশ করতে সমস্যা হয়েছে।"
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            clearBanners()
            val result = repository.deleteFeedPost(postId)
            result.onSuccess {
                _feedbackBanner.value = "পোস্টটি মুছে ফেলা হয়েছে।"
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "মুছে ফেলতে সমস্যা হয়েছে।"
            }
        }
    }

    fun toggleBookmark(post: FeedPost, isCurrentlyBookmarked: Boolean) {
        viewModelScope.launch {
            clearBanners()
            val result = repository.toggleBookmark(post, isCurrentlyBookmarked)
            result.onSuccess { added ->
                _feedbackBanner.value = if (added) {
                    "পছন্দের খেদমত তালিকায় সংরক্ষিত হয়েছে।"
                } else {
                    "পছন্দের খেদমত তালিকা থেকে সরানো হয়েছে।"
                }
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "সংরক্ষণ করা যায়নি।"
            }
        }
    }

    fun registerMadrasa(
        name: String,
        district: String,
        address: String,
        establishedYear: String,
        highestMarhala: String,
        facilities: String,
        studentCount: String,
        contactPhone: String,
        onSuccessNavigate: () -> Unit = {}
    ) {
        if (name.isBlank() || district.isBlank()) {
            _errorBanner.value = "অনুগ্রহ করে মাদরাসার নাম ও জেলা উল্লেখ করুন।"
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            clearBanners()
            val result = repository.createMadrasa(
                name = name,
                district = district,
                address = address,
                establishedYear = establishedYear,
                highestMarhala = highestMarhala,
                facilities = facilities,
                studentCount = studentCount,
                contactPhone = contactPhone
            )
            _isSubmitting.value = false
            result.onSuccess {
                _feedbackBanner.value = "আলহামদুলিল্লাহ, মাদরাসার তথ্য ডিরেক্টরিতে যুক্ত হয়েছে।"
                onSuccessNavigate()
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "মাদরাসা যুক্ত করতে ত্রুটি হয়েছে।"
            }
        }
    }

    fun submitKhedmotApplication(
        post: FeedPost,
        applicantName: String,
        applicantQualification: String,
        applicantExperience: String,
        applicantPhone: String,
        coverNote: String,
        onComplete: () -> Unit = {}
    ) {
        if (applicantName.isBlank() || applicantPhone.isBlank()) {
            _errorBanner.value = "অনুগ্রহ করে আপনার নাম ও যোগাযোগের নম্বর উল্লেখ করুন।"
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            clearBanners()
            val result = repository.submitApplicationToCircular(
                post = post,
                applicantName = applicantName,
                applicantQualification = applicantQualification,
                applicantExperience = applicantExperience,
                applicantPhone = applicantPhone,
                coverNote = coverNote
            )
            _isSubmitting.value = false
            result.onSuccess {
                _feedbackBanner.value = "আলহামদুলিল্লাহ, আপনার খেদমতের আবেদন সংশ্লিষ্ট মাদরাসায় প্রেরিত হয়েছে।"
                _selectedPostForApply.value = null
                onComplete()
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "আবেদন প্রেরণে ত্রুটি হয়েছে।"
            }
        }
    }

    fun updateReceivedSubmissionStatus(submission: KhedmotSubmission, newStatus: String) {
        viewModelScope.launch {
            clearBanners()
            val actorName = (userProfileState.value as? UiState.Success)?.data?.fullName
                ?.takeIf { it.isNotBlank() } ?: currentUserDisplayName.ifBlank { "মাদরাসা কর্তৃপক্ষ" }
            val result = repository.updateSubmissionStatus(submission, newStatus, actorName)
            result.onSuccess {
                _feedbackBanner.value = "আবেদনের অবস্থা হালনাগাদ করা হয়েছে এবং প্রার্থীকে নোটিফিকেশন পাঠানো হয়েছে।"
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "অবস্থা পরিবর্তনে ত্রুটি হয়েছে।"
            }
        }
    }

    fun sendMessage(
        receiverId: String,
        receiverName: String,
        subject: String,
        body: String,
        onSent: () -> Unit = {}
    ) {
        if (body.isBlank()) {
            _errorBanner.value = "অনুগ্রহ করে মেসেজের মূল বক্তব্য লিখুন।"
            return
        }
        viewModelScope.launch {
            _isSubmitting.value = true
            clearBanners()
            val senderName = (userProfileState.value as? UiState.Success)?.data?.fullName
                ?.takeIf { it.isNotBlank() } ?: currentUserDisplayName.ifBlank { "দ্বীনি ভাই" }
            val result = repository.sendDirectMessage(
                senderName = senderName,
                receiverId = receiverId,
                receiverName = receiverName,
                subject = subject.ifBlank { "খেদমত সংক্রান্ত যোগাযোগ" },
                body = body
            )
            _isSubmitting.value = false
            result.onSuccess {
                _feedbackBanner.value = "আলহামদুলিল্লাহ, আপনার পয়গাম প্রাপকের কাছে প্রেরিত হয়েছে।"
                onSent()
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "মেসেজ পাঠাতে ত্রুটি হয়েছে।"
            }
        }
    }

    fun markMessageRead(messageId: String) {
        viewModelScope.launch {
            repository.markMessageAsRead(messageId)
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationAsRead(notificationId)
        }
    }

    // Admin Actions
    fun adminVerifyMadrasa(madrasaId: String, verified: Boolean) {
        viewModelScope.launch {
            clearBanners()
            val result = repository.adminToggleMadrasaVerification(madrasaId, verified)
            result.onSuccess {
                _feedbackBanner.value = if (verified) {
                    "মাদরাসাটি 'যাচাইকৃত মাদরাসা' হিসেবে অনুমোদিত হয়েছে।"
                } else {
                    "মাদরাসার যাচাইকরণ স্ট্যাটাস পরিবর্তন করা হয়েছে।"
                }
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "অ্যাডমিন অনুমোদন সম্পন্ন হয়নি।"
            }
        }
    }

    fun adminVerifyPost(postId: String, isVerified: Boolean, approvalStatus: String) {
        viewModelScope.launch {
            clearBanners()
            val result = repository.adminUpdatePostVerification(postId, isVerified, approvalStatus)
            result.onSuccess {
                _feedbackBanner.value = "পোস্টের অনুমোদন অবস্থা হালনাগাদ হয়েছে।"
            }.onFailure { err ->
                _errorBanner.value = err.localizedMessage ?: "পোস্ট হালনাগাদ ব্যর্থ হয়েছে।"
            }
        }
    }

    private companion object {
        const val TAG = "QawmiViewModel"
    }
}
