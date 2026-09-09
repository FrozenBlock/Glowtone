if (glowtone_Context != 0) return vec3(0.0);

vec2 offset = glowtone_QuadOffset;
if (length(offset) < 1.0e-4) return vec3(0.0);

float vSpan = cross.w - cross.z;
if (vSpan <= 0.0) return vec3(0.0);

float rise = (PLANT_V_CENTRE - (glowtone_Uv.y - cross.z) / vSpan) * PLANT_HEIGHT;
vec3 corner = vec3(offset.x, rise, offset.y);

vec3 right = vec3(PLANT_AXIS_X, 0.0, PLANT_AXIS_Z);
vec3 forward = vec3(-PLANT_AXIS_Z, 0.0, PLANT_AXIS_X);

vec3 camRight = normalize(glowtone_CameraRight);
vec3 camUp = normalize(glowtone_CameraUp);
vec3 camForward = vec3(
	camRight.y * camUp.z - camRight.z * camUp.y,
	camRight.z * camUp.x - camRight.x * camUp.z,
	camRight.x * camUp.y - camRight.y * camUp.x
);

vec3 target = dot(corner, right) * camRight + corner.y * camUp + dot(corner, forward) * camForward;

return (target - corner) * PLANT_STRENGTH;
