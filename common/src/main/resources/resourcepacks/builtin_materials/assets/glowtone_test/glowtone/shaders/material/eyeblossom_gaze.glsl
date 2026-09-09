if (glowtone_Context != 0) return vec3(0.0);

vec2 offset = glowtone_QuadOffset;
if (length(offset) < 1.0e-4) return vec3(0.0);

vec3 pivot = glowtone_BlockPos + glowtone_LocalPos - vec3(offset.x, 0.0, offset.y);
vec2 toCamera = glowtone_CameraPos.xz - pivot.xz;
float distance = length(toCamera);
if (distance < 1.0e-4) return vec3(0.0);

vec2 facing = toCamera / distance;
vec2 across = vec2(-facing.y, facing.x);
vec2 authored = vec2(EYE_AXIS_X, EYE_AXIS_Z);

vec2 eyeInset = vec2(eyeblossom_eye.y - eyeblossom_eye.x, eyeblossom_eye.w - eyeblossom_eye.z) * 0.01;
vec2 bloomInset = vec2(eyeblossom_eye_bloom.y - eyeblossom_eye_bloom.x, eyeblossom_eye_bloom.w - eyeblossom_eye_bloom.z) * 0.01;

bool onEye = glowtone_Uv.x >= eyeblossom_eye.x + eyeInset.x && glowtone_Uv.x <= eyeblossom_eye.y - eyeInset.x
	&& glowtone_Uv.y >= eyeblossom_eye.z + eyeInset.y && glowtone_Uv.y <= eyeblossom_eye.w - eyeInset.y;
bool onBloom = glowtone_Uv.x >= eyeblossom_eye_bloom.x + bloomInset.x && glowtone_Uv.x <= eyeblossom_eye_bloom.y - bloomInset.x
	&& glowtone_Uv.y >= eyeblossom_eye_bloom.z + bloomInset.y && glowtone_Uv.y <= eyeblossom_eye_bloom.w - bloomInset.y;

if (!onEye && !onBloom) {
	float turn = atan(across.y, across.x) - mod(atan(EYE_AXIS_Z, EYE_AXIS_X), 1.57079633);
	float petalCos = cos(turn);
	float petalSin = sin(turn);

	return vec3(
		offset.x * petalCos - offset.y * petalSin - offset.x,
		0.0,
		offset.x * petalSin + offset.y * petalCos - offset.y
	);
}

vec2 blended = mix(authored, across, smoothstep(EYE_STEADY_NEAR, EYE_STEADY_FAR, distance));
float blendLength = length(blended);
blended = blendLength > 1.0e-3 ? blended / blendLength : across;

float turnCos = dot(authored, blended);
float turnSin = authored.x * blended.y - authored.y * blended.x;

vec3 shift = vec3(
	offset.x * turnCos - offset.y * turnSin - offset.x,
	0.0,
	offset.x * turnSin + offset.y * turnCos - offset.y
);

vec4 rect = onEye ? eyeblossom_eye : eyeblossom_eye_bloom;
float vSpan = rect.w - rect.z;
if (vSpan <= 0.0) return shift;

float rise = (EYE_V_CENTRE - (glowtone_Uv.y - rect.z) / vSpan) * EYE_HEIGHT;
vec2 normal = vec2(-blended.y, blended.x);
float lean = dot(normal, facing) > 0.0 ? -1.0 : 1.0;
float height = glowtone_CameraPos.y - (glowtone_BlockPos.y + glowtone_LocalPos.y - rise);
float pitch = atan(height, distance) * EYE_PITCH * lean;

vec3 tilted = cos(pitch) * vec3(0.0, 1.0, 0.0) + sin(pitch) * vec3(normal.x, 0.0, normal.y);

return shift + rise * (tilted - vec3(0.0, 1.0, 0.0));
