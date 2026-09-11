if (glowtone_Context != 0) return vec3(0.0);

vec3 axis = vec3(AXIS_X, AXIS_Y, AXIS_Z);
float axisLength = length(axis);
if (axisLength < 1.0e-4) return vec3(0.0);
axis /= axisLength;

vec3 corner = glowtone_QuadOffset;
float along = dot(corner, axis);
vec3 radial = corner - axis * along;
float reach = length(radial);
if (reach < 1.0e-4) return vec3(0.0);

vec3 direction = radial / reach;
float side = direction.x;
if (abs(side) < 0.1) side = direction.z;
if (abs(side) < 0.1) side = direction.y;
float span = side < 0.0 ? -reach : reach;

vec3 eye = glowtone_CameraPos - (glowtone_BlockPos + glowtone_LocalPos - corner);
float rise = dot(eye, axis);
vec3 level = eye - axis * rise;
float distance = length(level);
if (distance < 1.0e-4) return vec3(0.0);
vec3 facing = level / distance;

vec3 right;
vec3 up = axis;
if (ALIGN > 0.5) {
	vec3 level_right = glowtone_CameraRight - axis * dot(glowtone_CameraRight, axis);
	float rightLength = length(level_right);
	if (rightLength < 1.0e-4) return vec3(0.0);
	right = normalize(mix(level_right / rightLength, glowtone_CameraRight, PITCH));
	vec3 screenUp = mix(axis, glowtone_CameraUp, PITCH);
	up = normalize(screenUp - right * dot(screenUp, right));
} else {
	right = cross(axis, facing);
	if (PITCH > 0.0) {
		float tilt = atan(rise, distance) * PITCH;
		up = cos(tilt) * axis - sin(tilt) * facing;
	}
}

return right * span + up * along - corner;
