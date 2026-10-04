export type Gender = 'MALE' | 'FEMALE' | 'NON_BINARY' | 'OTHER' | 'PREFER_NOT_TO_SAY';

export interface AuthUser {
  id: number;
  firstName: string;
  lastName: string;
  username: string;
  email: string;
  roles: string[];
}

export interface AuthResponse {
  tokenType: 'Bearer';
  accessToken: string;
  accessTokenExpiresIn: number;
  refreshToken: string;
  user: AuthUser;
}

export interface RegisterPayload {
  firstName: string;
  lastName: string;
  username: string;
  email: string;
  password: string;
  confirmPassword: string;
  gender: Gender;
  bio?: string;
  isPhotographer: boolean;
  profilePicture?: string;
}

export interface UserProfile {
  id: number;
  firstName: string;
  lastName: string;
  username: string;
  gender: Gender;
  bio: string | null;
  profilePicture: string | null;
  isPhotographer: boolean;
  postCount: number;
  buddyCount: number;
  createdAt: string;
}

export interface UpdateProfilePayload {
  firstName: string;
  lastName: string;
  bio: string | null;
  gender: Gender;
  isPhotographer: boolean;
  profilePicture: string | null;
}

export interface UserLocation {
  latitude: number;
  longitude: number;
  accuracy: number;
  locationEnabled: boolean;
  updatedAt: string;
}

export interface NearbyUser {
  userId: number;
  firstName: string;
  lastName: string;
  username: string;
  profilePicture: string | null;
  bio: string | null;
  isPhotographer: boolean;
  distanceKm: number;
  approximateLatitude: number;
  approximateLongitude: number;
}

export interface BuddyUserSummary {
  id: number;
  username: string;
  firstName: string;
  lastName: string;
  profilePicture: string | null;
}

export interface BuddyRequest {
  id: number;
  sender: BuddyUserSummary;
  receiver: BuddyUserSummary;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'CANCELLED';
  createdAt: string;
  updatedAt: string;
}

export interface BuddyMatch {
  id: number;
  user: BuddyUserSummary;
  distanceKm: number | null;
  matchedAt: string;
}

export type PostStyle = 'PORTRAIT' | 'LANDSCAPE' | 'STREET' | 'NATURE' | 'FASHION' | 'TRAVEL' | 'OTHER';

export interface PostUser {
  id: number;
  username: string;
  firstName: string;
  lastName: string;
  profilePicture: string | null;
}

export interface PostItem {
  postId: number;
  user: PostUser;
  imageUrl: string;
  caption: string | null;
  style: PostStyle;
  likeCount: number;
  commentCount: number;
  createdAt: string;
  likedByCurrentUser: boolean;
}

export interface PostPage {
  content: PostItem[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface PostComment {
  id: number;
  user: PostUser;
  content: string;
  createdAt: string;
}

export interface CommentPage {
  content: PostComment[];
  totalPages: number;
  number: number;
  last: boolean;
}
